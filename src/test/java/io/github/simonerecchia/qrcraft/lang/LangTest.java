package io.github.simonerecchia.qrcraft.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Every language says every message, with the same placeholders, so no player ever gets a key or a format error. */
class LangTest {
	private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?s|%%");

	@Test
	void everyLanguageHasEveryKeyAndNoOthers() {
		Map<String, String> english = Lang.read(Lang.ENGLISH);
		for (String code : Lang.LANGUAGES) {
			assertEquals(english.keySet(), Lang.read(code).keySet(), code);
		}
	}

	@Test
	void everyTranslationTakesTheSameArguments() {
		Map<String, String> english = Lang.read(Lang.ENGLISH);
		for (String code : Lang.LANGUAGES) {
			Map<String, String> table = Lang.read(code);
			for (String key : english.keySet()) {
				assertEquals(arguments(english.get(key)), arguments(table.get(key)), code + " " + key);
			}
		}
	}

	/** The argument each placeholder reads, in order: {@code %s} takes the next one, {@code %2$s} the second. */
	private static List<Integer> arguments(String pattern) {
		List<Integer> used = new ArrayList<>();
		Matcher m = PLACEHOLDER.matcher(pattern);
		int next = 1;
		while (m.find()) {
			if (m.group().equals("%%")) continue;
			used.add(m.group(1) != null ? Integer.parseInt(m.group(1)) : next++);
		}
		return used.stream().distinct().sorted().toList();
	}

	@Test
	void everyMessageFormats() {
		Lang lang = Lang.load();
		for (String code : Lang.LANGUAGES) {
			for (String key : Lang.read(Lang.ENGLISH).keySet()) {
				String text = lang.format(code, key, "A", "B", "C");
				assertFalse(text.contains("%"), code + " " + key + ": " + text);
			}
		}
	}

	@Test
	void aClientGetsItsLanguageOrTheNearestOne() {
		Lang lang = Lang.load();
		assertEquals("it_it", lang.pick("it_it", "en_us"));
		assertEquals("it_it", lang.pick("IT_IT", "en_us"));
		assertEquals("it_it", lang.pick("it_ch", "en_us"));
		assertEquals("en_us", lang.pick("en_gb", "it_it"));
		assertEquals("it_it", lang.pick("de_de", "it_it"));
		assertEquals("en_us", lang.pick("de_de", "xx_yy"));
		assertEquals("en_us", lang.pick(null, null));
	}

	@Test
	void theSameMessageReadsInBothLanguages() {
		Lang lang = Lang.load();
		assertEquals("Wait 3 s before making another QR code.", lang.format("en_us", "qrcraft.error.cooldown", 3));
		assertEquals("Aspetta 3 s prima di creare un altro QR code.", lang.format("it_it", "qrcraft.error.cooldown", 3));
		assertEquals("Riga 1, colonna 2 di un poster 3×3", lang.format("it_it", "qrcraft.item.position", 1, 2, 3));
		assertTrue(lang.has("it_it", "qrcraft.created"));
	}
}
