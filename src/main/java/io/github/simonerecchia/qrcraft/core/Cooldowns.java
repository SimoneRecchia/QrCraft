package io.github.simonerecchia.qrcraft.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * When each player last made a QR code.  Every new code is a new file in the world's data folder, so on a public
 * server the command is rate-limited; the times are kept in memory only, and a restart forgives everyone.
 */
public final class Cooldowns {
	private final Map<UUID, Long> last = new HashMap<>();

	/** Milliseconds {@code player} still has to wait at {@code now}; 0 when they may go ahead. */
	public long remaining(UUID player, long now, long cooldownMillis) {
		Long then = last.get(player);
		if (then == null) return 0;
		return Math.max(0, then + cooldownMillis - now);
	}

	public void mark(UUID player, long now) {
		last.put(player, now);
	}

	/** The wait as whole seconds for a message, rounded up so "0 s" is never shown. */
	public static long seconds(long millis) {
		return (millis + 999) / 1000;
	}
}
