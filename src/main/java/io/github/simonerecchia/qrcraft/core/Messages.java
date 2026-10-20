package io.github.simonerecchia.qrcraft.core;

import io.github.simonerecchia.qrcraft.lang.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Messages and item names in the language of the player they are for.  Always literal text: a translatable component
 * would reach a client without the mod as its bare key.
 */
public final class Messages {
	private static final Lang LANG = Lang.load();

	private Messages() {
	}

	/** The language to answer {@code source} in: its client's, when it is a player, else the configured fallback. */
	public static String language(CommandSourceStack source) {
		String client = source.getEntity() instanceof ServerPlayer player ? player.getLanguage() : null;
		return LANG.pick(client, ServerConfig.fallbackLanguage());
	}

	public static String text(String language, String key, Object... args) {
		return LANG.format(language, key, args);
	}

	public static MutableComponent line(String language, String key, Object... args) {
		return Component.literal(text(language, key, args));
	}

	public static MutableComponent warning(String language, String key, Object... args) {
		return line(language, key, args).withStyle(ChatFormatting.YELLOW);
	}

	/** Item names and lore show in italics unless told otherwise. */
	public static MutableComponent upright(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(style -> style.withItalic(false).withColor(color));
	}
}
