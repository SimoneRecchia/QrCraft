// Reads a wall of map colours the way a phone reads a screenshot of it: every map pixel blown up to a few screen
// pixels, the background white and everything else black, then jsQR.

import jsQR from 'jsqr'

export const MAP = 128
const ZOOM = 4

/** The text in the code on {@code tiles}, rows of 128 x 128 map colours; null when there is no code to read. */
export function scanWall (tiles) {
  const side = tiles.length
  // the corner is always quiet zone: whatever colour it is, is the background
  const background = tiles[0][0][0]
  const width = side * MAP * ZOOM
  const rgba = new Uint8ClampedArray(width * width * 4)
  for (let y = 0; y < width; y++) {
    for (let x = 0; x < width; x++) {
      const mx = Math.floor(x / ZOOM)
      const my = Math.floor(y / ZOOM)
      const color = tiles[Math.floor(my / MAP)][Math.floor(mx / MAP)][(mx % MAP) + (my % MAP) * MAP]
      const value = color === background ? 255 : 0
      rgba.set([value, value, value, 255], (x + y * width) * 4)
    }
  }
  return jsQR(rgba, width, width)?.data ?? null
}
