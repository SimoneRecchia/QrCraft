package io.github.simonerecchia.qrcraft.qr;

/**
 * Whether two map colours make a code a phone can read.  The ratio is WCAG's: from 1 for the same colour to 21 for
 * black on white.  Below {@value #MINIMUM} a camera in a dim room loses the edges of the modules.
 */
public final class Contrast {
	public static final double MINIMUM = 3.0;

	private Contrast() {
	}

	/** The contrast ratio of two {@code 0xRRGGBB} colours, the same whichever is given first. */
	public static double ratio(int rgbA, int rgbB) {
		double a = luminance(rgbA);
		double b = luminance(rgbB);
		return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
	}

	/** A light code on a dark ground: a valid QR code, but a negative that a scanner looking for dark modules misses. */
	public static boolean inverted(int dark, int light) {
		return luminance(dark) > luminance(light);
	}

	/** Relative luminance of a {@code 0xRRGGBB} colour, 0 for black and 1 for white. */
	static double luminance(int rgb) {
		return 0.2126 * channel(rgb >> 16) + 0.7152 * channel(rgb >> 8) + 0.0722 * channel(rgb);
	}

	private static double channel(int value) {
		double c = (value & 0xFF) / 255.0;
		return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}
}
