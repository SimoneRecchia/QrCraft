// Prints the text of the QR code in a picture, read the way a phone reads a photo of a screen.
//
//   convert screenshot.png ppm:- > shot.ppm && node scan-image.mjs shot.ppm
//
// The picture is a binary PPM (P6), which ImageMagick writes and is simple enough to read here without a PNG library.

import fs from 'node:fs'
import jsQR from 'jsqr'

const file = fs.readFileSync(process.argv[2])
// header: P6, width, height, maximum value, each separated by whitespace, then one byte of whitespace
const header = file.toString('latin1', 0, 64).match(/^P6\s+(\d+)\s+(\d+)\s+(\d+)\s/)
if (!header || header[3] !== '255') throw new Error('not an 8-bit binary PPM')
const width = Number(header[1])
const height = Number(header[2])
const rgb = file.subarray(header[0].length)
const rgba = new Uint8ClampedArray(width * height * 4)
for (let i = 0; i < width * height; i++) {
  rgba[i * 4] = rgb[i * 3]
  rgba[i * 4 + 1] = rgb[i * 3 + 1]
  rgba[i * 4 + 2] = rgb[i * 3 + 2]
  rgba[i * 4 + 3] = 255
}
console.log(jsQR(rgba, width, height)?.data ?? '')
