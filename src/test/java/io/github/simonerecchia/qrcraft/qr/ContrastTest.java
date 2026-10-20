package io.github.simonerecchia.qrcraft.qr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Against the map colours of the dyes: COLOR_BLACK 0x191919, SNOW 0xFFFFFF, COLOR_YELLOW 0xE5E533, COLOR_BLUE 0x334CB2. */
class ContrastTest {
	@Test
	void blackOnWhiteIsTheBestThereIs() {
		assertEquals(17.4, Contrast.ratio(0x191919, 0xFFFFFF), 0.3);
		assertEquals(Contrast.ratio(0xFFFFFF, 0x191919), Contrast.ratio(0x191919, 0xFFFFFF), 1e-12);
	}

	@Test
	void theSameColourHasNone() {
		assertEquals(1.0, Contrast.ratio(0x334CB2, 0x334CB2), 1e-12);
	}

	@Test
	void yellowOnWhiteIsTooFaint() {
		assertTrue(Contrast.ratio(0xE5E533, 0xFFFFFF) < Contrast.MINIMUM);
		assertTrue(Contrast.ratio(0x334CB2, 0xE5E533) >= Contrast.MINIMUM);
	}

	@Test
	void aLightCodeOnADarkGroundIsInverted() {
		assertTrue(Contrast.inverted(0xFFFFFF, 0x191919));
		assertFalse(Contrast.inverted(0x191919, 0xFFFFFF));
	}
}
