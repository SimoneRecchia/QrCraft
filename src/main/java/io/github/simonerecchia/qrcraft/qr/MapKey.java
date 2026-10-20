package io.github.simonerecchia.qrcraft.qr;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The name under which a set of QR maps is remembered, so the same request gets the same maps back: they stack, and
 * the world does not collect a map file per command.  Everything that changes a pixel is in it.
 */
public final class MapKey {
	/** Raised when the drawing changes, so maps drawn the old way are not handed out for new requests. */
	private static final int FORMAT = 1;

	private MapKey() {
	}

	public static String of(String text, int grid, String ecc, byte dark, byte light) {
		String material = FORMAT + "\n" + grid + "\n" + ecc + "\n" + dark + "\n" + light + "\n" + text;
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("every Java has SHA-256", e);
		}
	}
}
