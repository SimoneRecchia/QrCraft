# QrCraft

Any text, link or book as a QR code on a Minecraft map: type `/qr` and the text, take the map in your hand, hang it
in an item frame, and a phone reads it off the wall.

* **Minecraft** 1.20.1 only, **Forge** 47.4.0 or later within 47 (`mods.toml` asks for `[47.4,48)`), **Java** 17
* **Server-side only.** On a server, only the server needs the jar: players join with a vanilla client, or a Forge
  one without QrCraft, and see and scan the maps all the same. In singleplayer, put it in your own `mods` folder.

## Commands

| Command | What you get |
|---|---|
| `/qr <text>` | a QR code on one map, in your hand |
| `/qr poster <size> <text>` | the code over a `size`×`size` wall of maps (up to 4×4 by default), named by row and column |
| `/qr color <code> <background> <text>` | in two of the sixteen dye colours, e.g. `/qr color blue white …` |
| `/qr poster <size> color <code> <background> <text>` | both |
| `/qr book` | the Book and Quill or Written Book in your hand, on the smallest wall it reads well on |
| `/qr book color <code> <background>` | the same, in colour |
| `/qr text <text>` | for a text that starts with `poster`, `color`, `book`, `text` or `help` |
| `/qr`, `/qr help` | the list above, in your language |

The text is anything a phone understands: a link, a plain sentence, a code, `WIFI:T:WPA;S:<network>;P:<password>;;`
to join a Wi-Fi network, `mailto:` or `tel:`. A chat line stops at 256 characters; a book has no such limit, and
the largest code, 2,331 bytes at the default error correction, fits on a 4×4 poster.

A command block reaches a player with `execute as`, e.g. `/execute as @p run qr https://example.com`.

## How the maps work

* They are **ordinary filled maps**, locked and without a position, so they never redraw from the world, a cartography
  table cannot zoom them, and an item frame shows them without a marker. A cartography table or a crafting table copies
  them like any map.
* **The same request gives the same map back**, so copies stack and the world keeps one map file per code, not one per
  command.
* A code fills its map as far as it can: a short text gets modules of 5 map pixels, a link 3, a long text 1. When a code is that
  dense, the mod says so and suggests a poster.
* The map's name and lore show what it encodes; a poster's maps say which row and column each goes in.
* Messages and names are in the player's own language (English and Italian), sent as plain text so a client without
  the mod reads them.
* A **glow item frame** keeps the code readable at night.

## Configuration

The server settings are stored with the world, in `serverconfig/qrcraft-server.toml`, and take effect as soon as the
file is saved.

| Setting | Default | Range | What it does |
|---|---|---|---|
| `permissionLevel` | `0` | 0–4 | who may use `/qr`: 0 everyone, 2 operators only |
| `cooldownSeconds` | `3` | 0–3600 | the wait between two codes, so nobody fills the world's data folder; operators and the owner of a singleplayer world never wait |
| `consumeEmptyMaps` | `false` | | charge one empty map per map, as a cartography table would; creative players never pay |
| `maxPosterSize` | `4` | 1–8 | the largest poster, in maps a side |
| `errorCorrection` | `MEDIUM` | `LOW` … `HIGH` | the least error correction a code gets; it is raised to the highest that fits in the same size |
| `reuseIdenticalMaps` | `true` | | hand out the same maps again for the same text, size and colours |
| `fallbackLanguage` | `en_us` | `en_us`, `it_it` | the language for players whose client language has no translation |

## Building from source

You need a JDK 17. The Gradle wrapper fetches everything else.

```bash
./gradlew build        # compiles, runs the JUnit tests, writes build/libs/qrcraft-<version>.jar
./gradlew runClient    # a development client
./gradlew runServer    # a headless development server in run/

npm ci --prefix e2e && e2e/run.sh   # the jar on a real Forge server, against a vanilla client (needs Node 22)
```

The unit tests read every layout back with ZXing, from one map to a 4×4 poster. The end-to-end test installs a Forge
server, joins it with a vanilla 1.20.1 bot ([mineflayer](https://github.com/PrismarineJS/mineflayer)), runs every
command and scans the maps it is sent; the CI runs both on every pull request.

A release is cut from the Actions tab: run **release** with a version such as `0.1.0-beta.1` (a tag that does not
exist yet is made on `main`; `-alpha`, `-beta` and `-rc` are marked pre-release). It builds the jar at that version,
attaches it to the release, also as `qrcraft.jar`, and publishes it to GitHub Packages.

[CLAUDE.md](CLAUDE.md) is the guide to working on the mod.

## Documentation

* In Italian: [guida al gioco](docs/GUIDA.md)

## Credits

The QR codes are made by Project Nayuki's [QR Code generator library](https://www.nayuki.io/page/qr-code-generator-library)
1.8.0 (MIT licence), copied unchanged except for its package into `io.github.simonerecchia.qrcraft.qrcodegen` so the
jar needs nothing beside Forge. Each file keeps its copyright and licence notice.

## License

To be decided. The jar's `mods.toml` says the same (`mod_license` in `gradle.properties`); change both
together when one is chosen.
