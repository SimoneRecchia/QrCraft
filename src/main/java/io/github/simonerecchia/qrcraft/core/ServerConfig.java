package io.github.simonerecchia.qrcraft.core;

import io.github.simonerecchia.qrcraft.qr.QrPicture;
import io.github.simonerecchia.qrcraft.qrcodegen.QrCode;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * The server-side settings, stored with the world in {@code serverconfig/qrcraft-server.toml}.
 *
 * Every value is read when it is used, so an edit to the file takes effect while the world is open.  Before the file
 * is loaded (the command tree is built a moment earlier) a value reads as its default instead of throwing.
 */
public final class ServerConfig {
	public static final ForgeConfigSpec SPEC;

	private static final ForgeConfigSpec.IntValue PERMISSION_LEVEL;
	private static final ForgeConfigSpec.IntValue MAX_POSTER_SIZE;
	private static final ForgeConfigSpec.IntValue COOLDOWN_SECONDS;
	private static final ForgeConfigSpec.BooleanValue CONSUME_EMPTY_MAPS;
	private static final ForgeConfigSpec.BooleanValue REUSE_IDENTICAL_MAPS;
	private static final ForgeConfigSpec.EnumValue<QrCode.Ecc> ERROR_CORRECTION;
	private static final ForgeConfigSpec.ConfigValue<String> FALLBACK_LANGUAGE;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		builder.push("access");
		PERMISSION_LEVEL = builder
				.comment("Permission level needed to use /qr: 0 is every player, 2 is operators only.",
						"0 is also what lets the owner of a singleplayer world without cheats use it.")
				.defineInRange("permissionLevel", 0, 0, 4);
		COOLDOWN_SECONDS = builder
				.comment("Seconds a player waits between two QR codes. Every new code is a file in the world's data folder,",
						"so this keeps one player from filling it. Operators and the owner of a singleplayer world never wait.")
				.defineInRange("cooldownSeconds", 3, 0, 3600);
		CONSUME_EMPTY_MAPS = builder
				.comment("Charge one empty map for every map a QR code is drawn on, as a cartography table would.",
						"Players in creative mode never pay.")
				.define("consumeEmptyMaps", false);
		builder.pop();

		builder.push("maps");
		MAX_POSTER_SIZE = builder
				.comment("Largest poster, in maps a side: /qr poster 4 makes a 4x4 wall of 16 maps.")
				.defineInRange("maxPosterSize", 4, 1, QrPicture.MAX_GRID);
		ERROR_CORRECTION = builder
				.comment("Least error correction a code gets. Higher survives a worse photo but needs more modules, which",
						"makes them smaller; a code is raised to the highest level that fits in the same size anyway.")
				.defineEnum("errorCorrection", QrCode.Ecc.MEDIUM);
		REUSE_IDENTICAL_MAPS = builder
				.comment("Hand out the same maps again for the same text, size and colours. The copies stack, and the world",
						"keeps one map file per code instead of one per command.")
				.define("reuseIdenticalMaps", true);
		builder.pop();

		builder.push("messages");
		FALLBACK_LANGUAGE = builder
				.comment("Language for players whose client language the mod has no translation for: en_us or it_it.")
				.define("fallbackLanguage", "en_us");
		builder.pop();

		SPEC = builder.build();
	}

	private ServerConfig() {
	}

	public static int permissionLevel() {
		return read(PERMISSION_LEVEL);
	}

	public static int cooldownSeconds() {
		return read(COOLDOWN_SECONDS);
	}

	public static boolean consumeEmptyMaps() {
		return read(CONSUME_EMPTY_MAPS);
	}

	public static int maxPosterSize() {
		return read(MAX_POSTER_SIZE);
	}

	public static QrCode.Ecc errorCorrection() {
		return read(ERROR_CORRECTION);
	}

	public static boolean reuseIdenticalMaps() {
		return read(REUSE_IDENTICAL_MAPS);
	}

	public static String fallbackLanguage() {
		return read(FALLBACK_LANGUAGE);
	}

	private static <T> T read(ForgeConfigSpec.ConfigValue<T> value) {
		return SPEC.isLoaded() ? value.get() : value.getDefault();
	}

	/**
	 * The file was edited: a new permission level changes which players see /qr at all, and a client only learns
	 * that when it is sent the command tree again.  Fired off the server thread, by the file watcher.
	 */
	public static void onReload(ModConfigEvent.Reloading event) {
		if (event.getConfig().getSpec() != SPEC) return;
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> server.getCommands().sendCommands(player)));
	}
}
