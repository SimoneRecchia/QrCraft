package io.github.simonerecchia.qrcraft.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The mod's messages, translated on the server.  A client without the mod has no language file for it, so a
 * translatable component would reach it as a bare key: every message goes out as plain text in the player's own
 * language, read from {@code assets/qrcraft/lang/<code>.json} in the mod's jar.
 */
public final class Lang {
	public static final String ENGLISH = "en_us";
	/** Every language with a file; LangTest holds them to the same keys and placeholders. */
	public static final List<String> LANGUAGES = List.of(ENGLISH, "it_it");

	private final Map<String, Map<String, String>> tables;

	private Lang(Map<String, Map<String, String>> tables) {
		this.tables = tables;
	}

	public static Lang load() {
		Map<String, Map<String, String>> tables = new HashMap<>();
		for (String code : LANGUAGES) tables.put(code, read(code));
		return new Lang(tables);
	}

	static Map<String, String> read(String code) {
		String path = "/assets/qrcraft/lang/" + code + ".json";
		try (InputStream in = Lang.class.getResourceAsStream(path)) {
			if (in == null) throw new IllegalStateException(path + " is missing from the jar");
			JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			Map<String, String> table = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : json.entrySet()) table.put(entry.getKey(), entry.getValue().getAsString());
			return table;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * The language to answer a client in: its own when there is a file for it, else another of the same language
	 * ({@code it_ch} gets {@code it_it}), else {@code fallback}, else English.
	 */
	public String pick(String language, String fallback) {
		String code = normalise(language);
		if (tables.containsKey(code)) return code;
		String prefix = code.split("_", 2)[0] + "_";
		for (String known : LANGUAGES) {
			if (known.startsWith(prefix)) return known;
		}
		String configured = normalise(fallback);
		return tables.containsKey(configured) ? configured : ENGLISH;
	}

	/** The message {@code key} in {@code language}, which must be one {@link #pick} returned, with its placeholders filled. */
	public String format(String language, String key, Object... args) {
		String pattern = tables.getOrDefault(language, tables.get(ENGLISH)).get(key);
		if (pattern == null) pattern = tables.get(ENGLISH).getOrDefault(key, key);
		return String.format(Locale.ROOT, pattern, args);
	}

	public boolean has(String language, String key) {
		Map<String, String> table = tables.get(language);
		return table != null && table.containsKey(key);
	}

	static String normalise(String code) {
		return code == null ? "" : code.trim().toLowerCase(Locale.ROOT).replace('-', '_');
	}
}
