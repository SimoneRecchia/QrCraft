package io.github.simonerecchia.qrcraft.qr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class MapKeyTest {
	private static final String BASE = MapKey.of("hello", 1, "MEDIUM", (byte) 118, (byte) 34);

	@Test
	void theSameRequestHasTheSameKey() {
		assertEquals(BASE, MapKey.of("hello", 1, "MEDIUM", (byte) 118, (byte) 34));
		assertEquals(64, BASE.length());
	}

	@Test
	void everythingThatChangesAPixelChangesTheKey() {
		Set<String> keys = Set.of(
				MapKey.of("hello!", 1, "MEDIUM", (byte) 118, (byte) 34),
				MapKey.of("hello", 2, "MEDIUM", (byte) 118, (byte) 34),
				MapKey.of("hello", 1, "HIGH", (byte) 118, (byte) 34),
				MapKey.of("hello", 1, "MEDIUM", (byte) 102, (byte) 34),
				MapKey.of("hello", 1, "MEDIUM", (byte) 118, (byte) 30));
		assertEquals(5, keys.size());
		for (String key : keys) assertNotEquals(BASE, key);
	}
}
