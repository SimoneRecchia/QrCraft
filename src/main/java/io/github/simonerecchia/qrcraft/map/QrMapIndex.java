package io.github.simonerecchia.qrcraft.map;

import io.github.simonerecchia.qrcraft.QrCraft;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Which maps already show which code, by {@link io.github.simonerecchia.qrcraft.qr.MapKey}: saved with the Overworld
 * in {@code data/qrcraft_maps.dat}, beside the map files themselves.
 */
public final class QrMapIndex extends SavedData {
	private static final String NAME = QrCraft.MOD_ID + "_maps";

	private final Map<String, int[]> maps = new HashMap<>();

	public static QrMapIndex of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(QrMapIndex::load, QrMapIndex::new, NAME);
	}

	private static QrMapIndex load(CompoundTag tag) {
		QrMapIndex index = new QrMapIndex();
		CompoundTag saved = tag.getCompound("maps");
		for (String key : saved.getAllKeys()) index.maps.put(key, saved.getIntArray(key));
		return index;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag saved = new CompoundTag();
		maps.forEach(saved::putIntArray);
		tag.put("maps", saved);
		return tag;
	}

	/** The maps that show {@code key}, tile by tile, or null when there are none - or one of them has been deleted. */
	@Nullable
	public int[] find(String key, ServerLevel overworld) {
		int[] ids = maps.get(key);
		if (ids == null) return null;
		for (int id : ids) {
			if (overworld.getMapData(MapItem.makeKey(id)) == null) {
				maps.remove(key);
				setDirty();
				return null;
			}
		}
		return ids.clone();
	}

	public void remember(String key, int[] ids) {
		maps.put(key, ids.clone());
		setDirty();
	}
}
