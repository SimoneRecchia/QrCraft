package io.github.simonerecchia.qrcraft.qr;

import java.util.ArrayList;
import java.util.List;

/**
 * What a QR map says about itself in its name and lore: a short preview of what it encodes.  Counted in code points,
 * so an emoji is never cut in half.
 */
public final class Labels {
	public static final String ELLIPSIS = "…";

	private Labels() {
	}

	/** The first non-blank line of {@code text}, its whitespace squeezed, cut to at most {@code max} code points. */
	public static String title(String text, int max) {
		for (String line : text.split("\\R")) {
			String squeezed = squeeze(line);
			if (!squeezed.isEmpty()) return cut(squeezed, max);
		}
		return "";
	}

	/** All of {@code text} on one line, word-wrapped to {@code width} code points and cut after {@code lines} lines. */
	public static List<String> preview(String text, int width, int lines) {
		List<String> all = wrap(squeeze(text), width);
		if (all.size() <= lines) return all;
		List<String> kept = new ArrayList<>(all.subList(0, lines));
		kept.set(lines - 1, ellipsize(kept.get(lines - 1), width));
		return kept;
	}

	/** {@code text} cut to at most {@code max} code points, the last of them an ellipsis when anything was cut. */
	public static String cut(String text, int max) {
		return length(text) <= max ? text : ellipsize(text, max);
	}

	/** {@code text} with an ellipsis after it, shortened so the two take at most {@code max} code points. */
	private static String ellipsize(String text, int max) {
		if (max <= 1) return ELLIPSIS;
		if (length(text) + 1 <= max) return text + ELLIPSIS;
		return text.substring(0, text.offsetByCodePoints(0, max - 1)).stripTrailing() + ELLIPSIS;
	}

	/** Greedy word wrap; a word longer than a line (a URL, usually) is split across lines. */
	private static List<String> wrap(String text, int width) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			if (word.isEmpty()) continue;
			if (line.length() > 0 && length(line) + 1 + length(word) <= width) {
				line.append(' ').append(word);
				continue;
			}
			if (line.length() > 0) {
				lines.add(line.toString());
				line.setLength(0);
			}
			while (length(word) > width) {
				int split = word.offsetByCodePoints(0, width);
				lines.add(word.substring(0, split));
				word = word.substring(split);
			}
			line.append(word);
		}
		if (line.length() > 0) lines.add(line.toString());
		return lines;
	}

	private static String squeeze(String text) {
		return text.strip().replaceAll("\\s+", " ");
	}

	private static int length(CharSequence text) {
		return Character.codePointCount(text, 0, text.length());
	}
}
