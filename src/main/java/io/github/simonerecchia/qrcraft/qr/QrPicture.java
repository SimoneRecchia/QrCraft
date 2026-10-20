package io.github.simonerecchia.qrcraft.qr;

import io.github.simonerecchia.qrcraft.qrcodegen.DataTooLongException;
import io.github.simonerecchia.qrcraft.qrcodegen.QrCode;
import io.github.simonerecchia.qrcraft.qrcodegen.QrSegment;

import java.util.List;
import java.util.Optional;

/**
 * A QR code laid out on a square wall of maps, {@link #grid} maps a side, each map {@value #MAP_PIXELS} pixels square.
 *
 * Every module is the same whole number of map pixels, so it stays square, and the code is centred with at least
 * {@value #QUIET} modules of background round it: a scanner finds the corner patterns by the light margin, and an item
 * frame's own border is no substitute for one.  Pixel {@code (x, y)} of a map is {@code colors[x + y * 128]}, x to the
 * right and y down, which is how the game draws a map whether it is held or framed.
 */
public final class QrPicture {
	public static final int MAP_PIXELS = 128;
	public static final int QUIET = 2;
	/** Modules of one map pixel scan from a screen only from close up; from two pixels on they read from across a room. */
	public static final int COMFORTABLE_MODULE = 2;
	/** A 1×1 to 8×8 wall: 64 maps already fill most of an inventory. */
	public static final int MAX_GRID = 8;

	private final QrCode code;
	private final int grid;
	private final int module;
	private final int offset;

	private QrPicture(QrCode code, int grid) {
		this.code = code;
		this.grid = grid;
		this.module = canvas(grid) / (code.size + 2 * QUIET);
		this.offset = (canvas(grid) - code.size * module) / 2;
	}

	/** The code for {@code text} on a {@code grid}×{@code grid} wall, or empty when it does not fit there. */
	public static Optional<QrPicture> encode(String text, int grid, QrCode.Ecc ecc) {
		if (grid < 1 || grid > MAX_GRID) throw new IllegalArgumentException("grid " + grid + " outside 1-" + MAX_GRID);
		// one segment in one mode, UTF-8 bytes unless it is all digits or all upper-case alphanumerics: mixed and kanji
		// segments save a few bits and are exactly what the odd phone scanner gets wrong
		List<QrSegment> segments = QrSegment.makeSegments(text);
		try {
			return Optional.of(new QrPicture(QrCode.encodeSegments(segments, ecc, QrCode.MIN_VERSION, maxVersion(grid), -1, true), grid));
		} catch (DataTooLongException e) {
			return Optional.empty();
		}
	}

	/**
	 * The smallest wall, up to {@code maxGrid} a side, whose modules are at least {@value #COMFORTABLE_MODULE} pixels;
	 * failing that the smallest the code fits on at all.  Empty when it fits on none.
	 */
	public static Optional<QrPicture> fit(String text, int maxGrid, QrCode.Ecc ecc) {
		QrPicture smallest = null;
		for (int grid = 1; grid <= maxGrid; grid++) {
			Optional<QrPicture> picture = encode(text, grid, ecc);
			if (picture.isEmpty()) continue;
			if (picture.get().module >= COMFORTABLE_MODULE) return picture;
			if (smallest == null) smallest = picture.get();
		}
		return Optional.ofNullable(smallest);
	}

	/** The side of the smallest wall, up to {@code maxGrid}, that {@code text} fits on; 0 when there is none. */
	public static int smallestGrid(String text, int maxGrid, QrCode.Ecc ecc) {
		for (int grid = 1; grid <= maxGrid; grid++) {
			if (encode(text, grid, ecc).isPresent()) return grid;
		}
		return 0;
	}

	/** The largest version whose modules, with the quiet zone, fit one pixel each across the wall. */
	static int maxVersion(int grid) {
		return Math.min(QrCode.MAX_VERSION, (canvas(grid) - 2 * QUIET - 17) / 4);
	}

	private static int canvas(int grid) {
		return grid * MAP_PIXELS;
	}

	public int grid() {
		return grid;
	}

	/** Map pixels a side of one module. */
	public int module() {
		return module;
	}

	/** Pixels from the wall's edge to the first module. */
	public int offset() {
		return offset;
	}

	/** Modules a side, not counting the quiet zone. */
	public int size() {
		return code.size;
	}

	public int version() {
		return code.version;
	}

	public QrCode.Ecc ecc() {
		return code.errorCorrectionLevel;
	}

	/** Whether pixel {@code (x, y)} of the whole wall, from its top left corner, is a dark module. */
	public boolean dark(int x, int y) {
		return code.getModule(Math.floorDiv(x - offset, module), Math.floorDiv(y - offset, module));
	}

	/** The colours of the map at {@code row}, {@code column} of the wall, counted from 0 at the top left. */
	public byte[] tile(int row, int column, byte dark, byte light) {
		if (row < 0 || row >= grid || column < 0 || column >= grid) {
			throw new IllegalArgumentException("tile " + row + "," + column + " outside a " + grid + "x" + grid + " wall");
		}
		byte[] colors = new byte[MAP_PIXELS * MAP_PIXELS];
		for (int y = 0; y < MAP_PIXELS; y++) {
			for (int x = 0; x < MAP_PIXELS; x++) {
				colors[x + y * MAP_PIXELS] = dark(column * MAP_PIXELS + x, row * MAP_PIXELS + y) ? dark : light;
			}
		}
		return colors;
	}
}
