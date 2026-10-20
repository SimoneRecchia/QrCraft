package io.github.simonerecchia.qrcraft.qr;

import java.util.List;

/**
 * The text a book encodes: its pages one after the other, a line break between two pages.  A book is how a player
 * gets more than the 256 characters a chat line allows into a QR code.
 */
public final class BookText {
	private BookText() {
	}

	/** The pages joined with a line break, blank pages and the blank ends of the whole dropped. */
	public static String join(List<String> pages) {
		StringBuilder text = new StringBuilder();
		for (String page : pages) {
			if (page.isBlank()) continue;
			if (text.length() > 0) text.append('\n');
			text.append(page.stripTrailing());
		}
		return text.toString().strip();
	}
}
