package io.github.simonerecchia.qrcraft.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CooldownsTest {
	private static final UUID ALEX = UUID.randomUUID();
	private static final UUID STEVE = UUID.randomUUID();

	@Test
	void aPlayerWhoNeverMadeOneNeedNotWait() {
		assertEquals(0, new Cooldowns().remaining(ALEX, 1_000, 3_000));
	}

	@Test
	void theWaitRunsDownAndIsPerPlayer() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.mark(ALEX, 10_000);
		assertEquals(3_000, cooldowns.remaining(ALEX, 10_000, 3_000));
		assertEquals(500, cooldowns.remaining(ALEX, 12_500, 3_000));
		assertEquals(0, cooldowns.remaining(ALEX, 13_000, 3_000));
		assertEquals(0, cooldowns.remaining(STEVE, 10_000, 3_000));
	}

	@Test
	void aWaitOfZeroNeverBlocks() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.mark(ALEX, 10_000);
		assertEquals(0, cooldowns.remaining(ALEX, 10_000, 0));
	}

	@Test
	void secondsRoundUp() {
		assertEquals(1, Cooldowns.seconds(1));
		assertEquals(3, Cooldowns.seconds(2_001));
		assertEquals(3, Cooldowns.seconds(3_000));
	}
}
