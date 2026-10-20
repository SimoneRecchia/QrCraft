# QrCraft — how this mod is built

Minecraft 1.20.1, Forge 47.4.0, Java 17, official mappings: the same toolchain as
[renewables](https://github.com/SimoneRecchia/renewables). `/qr` draws a QR code on locked vanilla maps.

## The one rule: the server alone

The mod's promise is that **players join without it**, so nothing may need a client to have the jar:

* **No registry entries** — no block, item, entity, sound, menu or packet channel. Everything is a vanilla
  `filled_map` with a locked `MapItemSavedData`.
* **No translatable components** for the mod's own text: a client without the mod shows the bare key. Messages go out
  as literal text in the player's language, from `assets/qrcraft/lang/*.json` via `Lang` (`Messages` picks the
  language from `ServerPlayer#getLanguage`). Vanilla keys (`permissions.requires.player`) are fine.
* **`displayTest="IGNORE_SERVER_VERSION"`** stays in `mods.toml`; `ModMetadataTest` fails the build without it.
* **No client-only class** may be referenced from common code; the mod has no client code at all.

The end-to-end test is what proves it: `e2e/run.sh` installs a real Forge server, puts the built jar in it and joins
with a vanilla 1.20.1 bot.

## Where the code is

| Package | What |
|---|---|
| `qrcraft` | `QrCraft`, the `@Mod` entry point: the config and the command, nothing else |
| `qrcraft.command` | `QrCommand`: the Brigadier tree, the checks (colours, poster size, cooldown, cost), handing the maps over |
| `qrcraft.map` | `QrMaps` (drawing maps, building the items, hand and inventory), `QrMapIndex` (identical requests share maps), `QrColors` (dye → map colour) |
| `qrcraft.core` | `ServerConfig` (per world, read on use), `Messages`, `Cooldowns` |
| `qrcraft.qr` | plain Java: `QrPicture` (the code laid out on a wall of 128-pixel maps), `Labels`, `BookText`, `Contrast`, `MapKey` |
| `qrcraft.lang` | plain Java: `Lang`, the server-side translations |
| `qrcraft.qrcodegen` | **vendored** Nayuki QR Code generator 1.8.0, MIT. Only the package line and imports differ from upstream; keep it that way so it can be replaced by a newer release wholesale |

A map pixel `(x, y)` is `colors[x + y * 128]`, x to the right and y down: how the game draws a map, held or framed.
`QrPictureTest.aTileIsTheWallReadRowByRow` holds the tiles to it.

## What must pass

```bash
./gradlew build                      # compiles and runs the JUnit tests, ZXing reading every layout back
npm ci --prefix e2e && e2e/run.sh    # the jar on a Forge server, a vanilla bot scanning what /qr sends it
```

The CI (`.github/workflows/build.yml`) runs both. `screenshot.yml`, run by hand, photographs a framed QR map with a real
vanilla client (`e2e/screenshot.sh`: Xvfb, Mesa, portablemc) and commits `docs/screenshot.png`; it fails unless the
picture scans. Plain Java is tested under `src/test/java` beside what it tests,
named `*Test`; what only a world can show goes in `e2e/test.mjs`. A new message goes in **every** language file —
`LangTest` checks the keys and the placeholders match.

A change to how a code is drawn raises `MapKey.FORMAT`, or players keep being handed maps drawn the old way.

## Comments and commits

As in renewables: a comment is a pointer — the constraint, the other file that has to agree, the trap — in a line or
two. Commit messages carry the reasoning in full prose; nothing is "improved", say what changed and what it fixes.
