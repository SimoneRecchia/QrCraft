package io.github.simonerecchia.qrcraft.qr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.simonerecchia.qrcraft.qrcodegen.QrCode;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Every layout the command can produce has to scan.  ZXing reads each one back from the map colours, from one map at
 * five pixels a module down to one pixel a module, and posters assembled from their tiles.
 */
class QrPictureTest {
	private static final QrCode.Ecc ECC = QrCode.Ecc.MEDIUM;

	private static QrPicture on(String text, int grid) {
		return QrPicture.encode(text, grid, ECC).orElseThrow(() -> new AssertionError(text.length() + " chars do not fit " + grid));
	}

	/** {@code length} characters of something like a sentence, so the code is in byte mode as a real text would be. */
	private static String text(int length) {
		String words = "the quick brown fox jumps over the lazy dog, città e perché ";
		StringBuilder out = new StringBuilder();
		while (out.length() < length) out.append(words);
		return out.substring(0, length);
	}

	@Test
	void aShortTextScansOffOneMapWithBigModules() {
		QrPicture picture = on("Hello QrCraft!", 1);
		assertEquals(1, picture.version());
		// 21 modules and 2 + 2 of quiet zone in 128 pixels
		assertEquals(5, picture.module());
		assertEquals("Hello QrCraft!", Scan.read(picture));
	}

	@Test
	void aLinkScans() {
		String url = "https://github.com/SimoneRecchia/QrCraft";
		assertEquals(url, Scan.read(on(url, 1)));
	}

	@Test
	void accentsAndEmojiSurviveAsUtf8() {
		String text = "Città più bella del mondo ☕ 🎉";
		assertEquals(text, Scan.read(on(text, 1)));
	}

	@Test
	void numbersAndCodesScan() {
		assertEquals("0123456789", Scan.read(on("0123456789", 1)));
		assertEquals("ABC-123 $%*+./:", Scan.read(on("ABC-123 $%*+./:", 1)));
	}

	@Test
	void oneMapTakesUpToVersion26AtOnePixelAModule() {
		assertEquals(26, QrPicture.maxVersion(1));
		assertEquals(40, QrPicture.maxVersion(2));
		// the longest text that still fits on one map: version 26 at one pixel a module, and it still has to scan
		int longest = 0;
		for (int length = 900; QrPicture.encode(text(length), 1, ECC).isPresent(); length++) longest = length;
		QrPicture densest = on(text(longest), 1);
		assertEquals(26, densest.version());
		assertEquals(1, densest.module());
		assertEquals(text(longest), Scan.read(densest));
		assertTrue(QrPicture.encode(text(longest + 1), 1, ECC).isEmpty());
	}

	@Test
	void everyChatLineFitsOnOneMap() {
		// the chat sends at most 256 characters, so "/qr " leaves 252: even in two-byte letters they fit
		assertTrue(QrPicture.encode("è".repeat(252), 1, ECC).isPresent());
	}

	@Test
	void postersScanAssembledFromTheirTiles() {
		for (int grid = 2; grid <= 4; grid++) {
			String text = text(300 * grid);
			QrPicture picture = on(text, grid);
			assertEquals(grid, picture.grid());
			assertEquals(text, Scan.read(picture), grid + "x" + grid);
		}
	}

	@Test
	void theLargestCodeScansOnAFourByFourPoster() {
		int longest = 0;
		for (int length = 2000; QrPicture.encode(text(length), 4, ECC).isPresent(); length++) longest = length;
		QrPicture picture = on(text(longest), 4);
		assertEquals(40, picture.version());
		// 177 modules and the quiet zone in 512 pixels
		assertEquals(2, picture.module());
		assertEquals(text(longest), Scan.read(picture));
	}

	@Test
	void theCodeIsCentredInsideItsQuietZone() {
		for (String text : new String[] {"a", text(100), text(600)}) {
			QrPicture picture = on(text, 1);
			int start = picture.offset();
			int end = start + picture.size() * picture.module();
			assertTrue(start >= QrPicture.QUIET * picture.module(), "quiet zone before the code");
			assertTrue(QrPicture.MAP_PIXELS - end >= QrPicture.QUIET * picture.module(), "quiet zone after the code");
			assertTrue(Math.abs(start - (QrPicture.MAP_PIXELS - end)) <= 1, "centred");
			for (int i = 0; i < QrPicture.MAP_PIXELS; i++) {
				assertFalse(picture.dark(i, start - 1) || picture.dark(start - 1, i) || picture.dark(i, end) || picture.dark(end, i),
						"nothing dark outside the code");
			}
			// the top-left finder pattern's outer ring starts exactly at the offset
			assertTrue(picture.dark(start, start));
		}
	}

	@Test
	void aTileIsTheWallReadRowByRow() {
		QrPicture picture = on(text(500), 2);
		byte[] tile = picture.tile(1, 0, Scan.DARK, Scan.LIGHT);
		for (int y = 0; y < QrPicture.MAP_PIXELS; y += 7) {
			for (int x = 0; x < QrPicture.MAP_PIXELS; x += 5) {
				// x is across and y is down, the game's colors[x + y * 128]
				byte expected = picture.dark(x, QrPicture.MAP_PIXELS + y) ? Scan.DARK : Scan.LIGHT;
				assertEquals(expected, tile[x + y * QrPicture.MAP_PIXELS], x + "," + y);
			}
		}
	}

	@Test
	void fitPrefersTheSmallestWallWithComfortableModules() {
		String dense = text(600);
		assertEquals(1, on(dense, 1).module());
		QrPicture fitted = QrPicture.fit(dense, 4, ECC).orElseThrow();
		assertEquals(2, fitted.grid());
		assertTrue(fitted.module() >= QrPicture.COMFORTABLE_MODULE);
		assertEquals(1, QrPicture.fit("short", 4, ECC).orElseThrow().grid());
	}

	@Test
	void fitSettlesForOnePixelModulesWhenNothingBiggerIsAllowed() {
		QrPicture fitted = QrPicture.fit(text(600), 1, ECC).orElseThrow();
		assertEquals(1, fitted.grid());
		assertEquals(1, fitted.module());
	}

	@Test
	void tooLongForEveryWall() {
		String huge = text(3000);
		assertEquals(Optional.empty(), QrPicture.fit(huge, 4, ECC));
		assertEquals(0, QrPicture.smallestGrid(huge, 4, ECC));
		assertEquals(2, QrPicture.smallestGrid(text(1500), 4, ECC));
	}
}
