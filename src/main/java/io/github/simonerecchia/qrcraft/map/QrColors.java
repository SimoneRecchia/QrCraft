package io.github.simonerecchia.qrcraft.map;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.MapColor;

/**
 * The colours a code can be drawn in: the sixteen dyes, by the names /give and the dyes themselves use.  Each is its
 * dye's map colour at full brightness, which is that colour exactly, so black is 0x191919 and white 0xFFFFFF.
 */
public final class QrColors {
	public static final DyeColor DARK = DyeColor.BLACK;
	public static final DyeColor LIGHT = DyeColor.WHITE;

	private QrColors() {
	}

	public static Optional<DyeColor> byName(String name) {
		return Optional.ofNullable(DyeColor.byName(name.toLowerCase(Locale.ROOT), null));
	}

	public static List<String> names() {
		return Arrays.stream(DyeColor.values()).map(DyeColor::getName).toList();
	}

	/** The byte a map stores for this colour. */
	public static byte packed(DyeColor dye) {
		return dye.getMapColor().getPackedId(MapColor.Brightness.HIGH);
	}

	/** The colour as {@code 0xRRGGBB}, the way the game draws it. */
	public static int rgb(DyeColor dye) {
		return dye.getMapColor().col;
	}
}
