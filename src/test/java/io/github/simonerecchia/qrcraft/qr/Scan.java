package io.github.simonerecchia.qrcraft.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.List;
import java.util.Map;

/**
 * Reads a wall of maps back the way a phone would: ZXing, on the picture the game draws, the map colours turned into
 * the RGB of black and white map pixels and every map pixel blown up to a few screen pixels.
 */
final class Scan {
	static final byte DARK = 118;   // COLOR_BLACK (29), brightness HIGH
	static final byte LIGHT = 34;   // SNOW (8), brightness HIGH
	private static final int DARK_RGB = 0x191919;
	private static final int LIGHT_RGB = 0xFFFFFF;
	private static final int ZOOM = 4;

	private Scan() {
	}

	/** The text in the code drawn on {@code picture}'s maps, assembled into one wall. */
	static String read(QrPicture picture) {
		int grid = picture.grid();
		int side = grid * QrPicture.MAP_PIXELS;
		byte[][] tiles = new byte[grid * grid][];
		for (int row = 0; row < grid; row++) {
			for (int column = 0; column < grid; column++) tiles[row * grid + column] = picture.tile(row, column, DARK, LIGHT);
		}
		int[] pixels = new int[side * ZOOM * side * ZOOM];
		for (int y = 0; y < side * ZOOM; y++) {
			for (int x = 0; x < side * ZOOM; x++) {
				int mx = x / ZOOM;
				int my = y / ZOOM;
				byte[] tile = tiles[(my / QrPicture.MAP_PIXELS) * grid + mx / QrPicture.MAP_PIXELS];
				byte color = tile[mx % QrPicture.MAP_PIXELS + (my % QrPicture.MAP_PIXELS) * QrPicture.MAP_PIXELS];
				pixels[x + y * side * ZOOM] = 0xFF000000 | (color == DARK ? DARK_RGB : LIGHT_RGB);
			}
		}
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(side * ZOOM, side * ZOOM, pixels)));
		Map<DecodeHintType, Object> hints = Map.of(
				DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE),
				DecodeHintType.CHARACTER_SET, "UTF-8",
				DecodeHintType.TRY_HARDER, Boolean.TRUE,
				// a plain photo: the pure-barcode shortcut would read a perfect render no camera ever sees
				DecodeHintType.PURE_BARCODE, Boolean.FALSE);
		try {
			return new QRCodeReader().decode(bitmap, hints).getText();
		} catch (NotFoundException | ChecksumException | FormatException e) {
			throw new AssertionError("the code on the " + grid + "x" + grid + " wall does not scan: " + e, e);
		}
	}
}
