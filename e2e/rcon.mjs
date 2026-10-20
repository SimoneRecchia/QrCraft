// The server console over RCON: the test gives items with it and run.sh stops the server with it.
//
//   node rcon.mjs <command>
//
// MC_HOST, RCON_PORT and RCON_PASSWORD as in test.mjs.

import net from 'node:net'
import { fileURLToPath } from 'node:url'

const HOST = process.env.MC_HOST ?? '127.0.0.1'
const PORT = Number(process.env.RCON_PORT ?? 25575)
const PASSWORD = process.env.RCON_PASSWORD ?? 'qrcraft-e2e'
const AUTH = 3
const COMMAND = 2

export function rcon (command) {
  return new Promise((resolve, reject) => {
    const socket = net.connect(PORT, HOST)
    let buffer = Buffer.alloc(0)
    let authed = false
    const packet = (id, type, body) => {
      const payload = Buffer.from(body, 'utf8')
      const out = Buffer.alloc(14 + payload.length)
      out.writeInt32LE(10 + payload.length, 0)
      out.writeInt32LE(id, 4)
      out.writeInt32LE(type, 8)
      payload.copy(out, 12)
      return out
    }
    socket.on('connect', () => socket.write(packet(1, AUTH, PASSWORD)))
    socket.on('data', (data) => {
      buffer = Buffer.concat([buffer, data])
      while (buffer.length >= 4 && buffer.length >= buffer.readInt32LE(0) + 4) {
        const length = buffer.readInt32LE(0)
        const id = buffer.readInt32LE(4)
        const body = buffer.toString('utf8', 12, 4 + length - 2)
        buffer = buffer.subarray(4 + length)
        if (!authed) {
          if (id === -1) return reject(new Error('RCON refused the password'))
          authed = true
          socket.write(packet(2, COMMAND, command))
        } else {
          socket.end()
          return resolve(body)
        }
      }
    })
    // "stop" closes the connection before it answers
    socket.on('close', () => resolve(''))
    socket.on('error', reject)
  })
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  console.log(await rcon(process.argv.slice(2).join(' ')))
}
