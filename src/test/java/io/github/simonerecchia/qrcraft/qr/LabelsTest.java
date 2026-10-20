package io.github.simonerecchia.qrcraft.qr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LabelsTest {
	@Test
	void theTitleIsTheFirstLineThatSaysSomething() {
		assertEquals("Hello world", Labels.title("\n   \n  Hello   world \nsecond line", 32));
	}

	@Test
	void aLongTitleIsCutWithAnEllipsis() {
		assertEquals("https://github.com/SimoneRecc…", Labels.title("https://github.com/SimoneRecchia/QrCraft", 30));
		assertEquals(30, Labels.title("x".repeat(100), 30).length());
	}

	@Test
	void anEmojiIsNeverCutInHalf() {
		String title = Labels.title("🎉".repeat(10), 5);
		assertEquals("🎉🎉🎉🎉…", title);
		assertEquals(5, title.codePointCount(0, title.length()));
	}

	@Test
	void shortTextIsLeftAlone() {
		assertEquals("ok", Labels.cut("ok", 2));
		assertEquals(List.of("one two"), Labels.preview("one   two", 20, 3));
	}

	@Test
	void thePreviewWrapsWordsAndSplitsAUrl() {
		assertEquals(List.of("visit", "https://example", ".com/a/b"), Labels.preview("visit https://example.com/a/b", 15, 3));
	}

	@Test
	void whatDoesNotFitEndsInAnEllipsis() {
		List<String> lines = Labels.preview("one two three four five six seven eight nine ten", 9, 2);
		assertEquals(List.of("one two", "three…"), lines);
		List<String> full = Labels.preview("abcdefghi jklmnopqr stuvwxyz", 9, 2);
		assertEquals("jklmnopq…", full.get(1));
		for (String line : full) assertTrue(line.length() <= 9);
	}
}
