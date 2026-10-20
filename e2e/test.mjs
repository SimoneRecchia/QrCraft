// Joins a Forge server running QrCraft as a vanilla 1.20.1 client - the mod is on the server only - and reads back,
// with jsQR, the maps /qr hands out: one map, a poster, colours, a book, the same text twice, Italian, the cooldown,
// the empty-map cost and the permission level, the last two by editing the world's config while the server runs.
//
//   node test.mjs             (e2e/run.sh starts the server and calls this)
//
// MC_HOST, MC_PORT, RCON_PORT, RCON_PASSWORD and SERVER_DIR say where the server is; the defaults are run.sh's.

import fs from 'node:fs'
import path from 'node:path'
import mineflayer from 'mineflayer'
import nbt from 'prismarine-nbt'
import { rcon } from './rcon.mjs'
import { MAP, scanWall } from './scan.mjs'

const HOST = process.env.MC_HOST ?? '127.0.0.1'
const PORT = Number(process.env.MC_PORT ?? 25565)
const SERVER_DIR = process.env.SERVER_DIR ?? 'server'
const CONFIG = path.join(SERVER_DIR, 'world', 'serverconfig', 'qrcraft-server.toml')
const NAME = 'QrBot'
// the server's default cooldown, plus margin
const COOLDOWN_MS = 3500

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

// every wait below has its own timeout; this is for a server that accepts the connection and then says nothing
setTimeout(() => {
  console.log('FAIL  the whole run took more than five minutes')
  process.exit(1)
}, 5 * 60 * 1000).unref()

let failures = 0
async function check (name, body) {
  try {
    await body()
    console.log(`ok    ${name}`)
  } catch (e) {
    failures++
    console.log(`FAIL  ${name}\n      ${e.stack ?? e}`)
  }
}

function assert (condition, message) {
  if (!condition) throw new Error(message)
}

function assertEqual (actual, expected, what) {
  if (actual !== expected) throw new Error(`${what}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`)
}

async function waitFor (what, test, timeout = 10000) {
  const end = Date.now() + timeout
  for (;;) {
    const value = test()
    if (value) return value
    if (Date.now() > end) throw new Error(`timed out waiting for ${what}`)
    await sleep(100)
  }
}

// --- the server's config file, which Forge reloads when it changes ------------------------------------------------

function setConfig (key, value) {
  const toml = fs.readFileSync(CONFIG, 'utf8')
  const pattern = new RegExp(`^(\\s*${key}\\s*=\\s*).*$`, 'm')
  assert(pattern.test(toml), `${CONFIG} has no ${key}`)
  fs.writeFileSync(CONFIG, toml.replace(pattern, `$1${value}`))
}

// --- the bot ---------------------------------------------------------------------------------------------------------

const bot = mineflayer.createBot({ host: HOST, port: PORT, username: NAME, version: '1.20.1', auth: 'offline' })
const maps = new Map()   // map id -> its 128 x 128 colours, as the client has been sent them
const messages = []      // every system message, in order

bot._client.on('map', (packet) => {
  if (!packet.columns) return
  const colors = maps.get(packet.itemDamage) ?? new Uint8Array(MAP * MAP).fill(255)
  for (let y = 0; y < packet.rows; y++) {
    for (let x = 0; x < packet.columns; x++) colors[packet.x + x + (packet.y + y) * MAP] = packet.data[x + y * packet.columns]
  }
  maps.set(packet.itemDamage, colors)
})
bot.on('messagestr', (text) => messages.push(text))
bot.on('kicked', (reason) => console.log(`kicked: ${JSON.stringify(reason)}`))
bot.on('error', (e) => console.log(`bot error: ${e.stack ?? e}`))

/** Runs a command and returns the first message after it that matches. */
async function command (line, match, timeout = 10000) {
  const from = messages.length
  bot.chat(`/${line}`)
  try {
    return await waitFor(`a reply to /${line} matching ${match}`, () => messages.slice(from).find((m) => match.test(m)), timeout)
  } catch (e) {
    e.message += `; the server said ${JSON.stringify(messages.slice(from))}`
    throw e
  }
}

function tag (item) {
  return item?.nbt ? nbt.simplify(item.nbt) : {}
}

function itemName (item) {
  const name = tag(item).display?.Name
  return name ? JSON.parse(name).text : undefined
}

function qrMaps () {
  return bot.inventory.items().filter((item) => item.name === 'filled_map' && tag(item).qrcraft)
}

/** The map's colours once the client has all of them. */
function colorsOf (id) {
  return waitFor(`the colours of map ${id}`, () => {
    const colors = maps.get(id)
    return colors && !colors.includes(255) ? colors : null
  })
}

/** Reads a wall of maps, given as rows of map ids. */
async function scan (wall) {
  const tiles = []
  for (const row of wall) tiles.push(await Promise.all(row.map(colorsOf)))
  const text = scanWall(tiles)
  assert(text !== null, `no QR code found on the ${wall.length}x${wall.length} wall`)
  return text
}

async function heldQr () {
  return waitFor('a QR map in the hand', () => (bot.heldItem?.name === 'filled_map' ? bot.heldItem : null))
}

// --- the tests -------------------------------------------------------------------------------------------------------

await new Promise((resolve, reject) => {
  bot.once('spawn', resolve)
  bot.once('end', (reason) => reject(new Error(`disconnected before spawning: ${reason}`)))
})
console.log(`ok    a vanilla 1.20.1 client joins the Forge server, as ${NAME}`)
await sleep(1000)

await check('/qr alone prints the help', async () => {
  await command('qr', /QrCraft: QR codes on maps/)
})

let hello
await check('/qr <text> puts a map in the hand that scans back to the text', async () => {
  await command('qr Hello QrCraft!', /Here is your QR code/)
  hello = await heldQr()
  assertEqual(itemName(hello), 'QR: Hello QrCraft!', 'the map\'s name')
  assertEqual(tag(hello).qrcraft.size, 1, 'the wall size in the map\'s tag')
  assertEqual(await scan([[tag(hello).map]]), 'Hello QrCraft!', 'the scanned text')
})

await check('a second code straight away waits for the cooldown', async () => {
  await command('qr too soon', /Wait \d+ s before making another QR code/)
})

await sleep(COOLDOWN_MS)
await check('the same text again is the same map, stacked in the hand', async () => {
  await command('qr Hello QrCraft!', /Here is your QR code/)
  const held = await waitFor('two maps in the hand', () => (bot.heldItem?.count === 2 ? bot.heldItem : null))
  assertEqual(tag(held).map, tag(hello).map, 'the map id')
})

await sleep(COOLDOWN_MS)
const url = 'https://github.com/SimoneRecchia/QrCraft'
await check('/qr poster 2 gives four maps that scan as one code', async () => {
  await command(`qr poster 2 ${url}`, /2×2 QR poster: 4 maps/)
  const tiles = await waitFor('four poster maps', () => {
    const found = qrMaps().filter((item) => tag(item).qrcraft.size === 2)
    return found.length === 4 ? found : null
  })
  const wall = [[0, 0], [0, 0]]
  for (const item of tiles) wall[tag(item).qrcraft.row][tag(item).qrcraft.column] = tag(item).map
  assertEqual(new Set(wall.flat()).size, 4, 'four different maps')
  assert(tiles.some((item) => itemName(item) === `QR 1/4: ${url.slice(0, 31)}…`), `a map named "QR 1/4: …" (${tiles.map(itemName)})`)
  assertEqual(await scan(wall), url, 'the scanned text')
})

await sleep(COOLDOWN_MS)
await check('/qr color draws in two dye colours and still scans', async () => {
  await command('qr color blue yellow Colours work', /Here is your QR code/)
  const held = await waitFor('the coloured map', () => (itemName(bot.heldItem) === 'QR: Colours work' ? bot.heldItem : null))
  const colors = await colorsOf(tag(held).map)
  // COLOR_BLUE (25) and COLOR_YELLOW (18) at full brightness
  assertEqual([...new Set(colors)].sort((a, b) => a - b).join(','), `${18 * 4 + 2},${25 * 4 + 2}`, 'the colours on the map')
  assertEqual(await scan([[tag(held).map]]), 'Colours work', 'the scanned text')
})

await sleep(COOLDOWN_MS)
await check('a player whose client is in Italian is answered in Italian', async () => {
  bot.setSettings({ locale: 'it_it' })
  await sleep(500)
  try {
    await command('qr Ciao mondo', /Ecco il tuo QR code/)
    const held = await waitFor('the Italian map', () => (itemName(bot.heldItem) === 'QR: Ciao mondo' ? bot.heldItem : null))
    assertEqual(await scan([[tag(held).map]]), 'Ciao mondo', 'the scanned text')
  } finally {
    // the checks after this one expect English
    bot.setSettings({ locale: 'en_US' })
    await sleep(500)
  }
})

await sleep(COOLDOWN_MS)
await check('/qr raw encodes a text that starts like a subcommand', async () => {
  await command('qr raw poster of the week', /Here is your QR code/)
  const held = await waitFor('the raw map', () => (itemName(bot.heldItem) === 'QR: poster of the week' ? bot.heldItem : null))
  assertEqual(await scan([[tag(held).map]]), 'poster of the week', 'the scanned text')
})

await sleep(COOLDOWN_MS)
await check('/qr book encodes the pages of the book in the hand', async () => {
  await rcon(`give ${NAME} writable_book{pages:["Line one","Line two"]}`)
  const book = await waitFor('the book', () => bot.inventory.items().find((item) => item.name === 'writable_book'))
  await bot.equip(book, 'hand')
  await command('qr book', /Here is your QR code/)
  // the hotbar is full by now (four poster maps among them), so the map goes into the inventory
  const map = await waitFor('the book\'s map', () => bot.inventory.items().find((item) => itemName(item) === 'QR: Line one'))
  assertEqual(await scan([[tag(map).map]]), 'Line one\nLine two', 'the scanned text')
})

await sleep(COOLDOWN_MS)
await check('with consumeEmptyMaps a code costs an empty map', async () => {
  setConfig('consumeEmptyMaps', 'true')
  await sleep(3000)
  await command('qr Paid for', /You need 1 empty map\(s\) in your inventory, and you have 0/)
  await rcon(`give ${NAME} map 1`)
  await waitFor('the empty map', () => bot.inventory.items().find((item) => item.name === 'map'))
  await command('qr Paid for', /Here is your QR code/)
  await waitFor('the empty map to be used up', () => !bot.inventory.items().some((item) => item.name === 'map'))
  await waitFor('the paid-for map', () => bot.inventory.items().find((item) => itemName(item) === 'QR: Paid for'))
})
setConfig('consumeEmptyMaps', 'false')

await sleep(COOLDOWN_MS)
await check('with permissionLevel 2 a player who is not an operator cannot use /qr', async () => {
  setConfig('permissionLevel', '2')
  await sleep(3000)
  await command('qr Not allowed', /Unknown or incomplete command/)
})
setConfig('permissionLevel', '0')

bot.quit()
await sleep(500)
console.log(failures === 0 ? '\nall end-to-end checks passed' : `\n${failures} end-to-end check(s) failed`)
process.exit(failures === 0 ? 0 : 1)
