package io.github.simonerecchia.qrcraft.map;

import io.github.simonerecchia.qrcraft.qr.QrPicture;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * The maps a code is drawn on, and the filled-map items that show them.  Vanilla through and through: a locked map
 * with no position tracking, so it never redraws from the world, a cartography table cannot zoom it, and an item frame
 * shows it without a marker.  Copies made at a cartography table are the same map.
 */
public final class QrMaps {
	/** The mod's own tag on every QR map item: which tile of which wall it is.  The game keeps it and ignores it. */
	public static final String TAG = "qrcraft";

	private QrMaps() {
	}

	/**
	 * The maps showing {@code picture}, one per tile in reading order: those already drawn for {@code key}, else new
	 * ones, then remembered under it.  No key, no remembering.
	 */
	public static int[] draw(MinecraftServer server, QrPicture picture, byte dark, byte light, @Nullable String key) {
		ServerLevel overworld = server.overworld();
		int grid = picture.grid();
		QrMapIndex index = key == null ? null : QrMapIndex.of(server);
		if (index != null) {
			int[] known = index.find(key, overworld);
			if (known != null && known.length == grid * grid) return known;
		}
		int[] ids = new int[grid * grid];
		for (int row = 0; row < grid; row++) {
			for (int column = 0; column < grid; column++) ids[row * grid + column] = draw(overworld, picture.tile(row, column, dark, light));
		}
		if (index != null) index.remember(key, ids);
		return ids;
	}

	private static int draw(ServerLevel overworld, byte[] colors) {
		// the centre and dimension are never read: a locked map without tracking draws nothing from the world
		MapItemSavedData data = MapItemSavedData.createFresh(0, 0, (byte) 0, false, false, Level.OVERWORLD).locked();
		System.arraycopy(colors, 0, data.colors, 0, colors.length);
		data.setDirty();
		int id = overworld.getFreeMapId();
		overworld.setMapData(MapItem.makeKey(id), data);
		return id;
	}

	/** A filled map showing map {@code id}, tile {@code row}, {@code column} of a {@code grid}-sided wall. */
	public static ItemStack item(int id, Component name, List<Component> lore, int markings, int row, int column, int grid) {
		ItemStack stack = new ItemStack(Items.FILLED_MAP);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putInt("map", id);
		CompoundTag tile = new CompoundTag();
		tile.putInt("row", row);
		tile.putInt("column", column);
		tile.putInt("size", grid);
		tag.put(TAG, tile);

		stack.setHoverName(name);
		CompoundTag display = stack.getOrCreateTagElement("display");
		// the lines drawn on the item's icon, in the colour of the code
		display.putInt("MapColor", markings);
		ListTag lines = new ListTag();
		for (Component line : lore) lines.add(StringTag.valueOf(Component.Serializer.toJson(line)));
		display.put("Lore", lines);
		return stack;
	}

	/**
	 * The map in the player's hand: the hand if it is empty, or already holds the same map; else a free hotbar slot,
	 * which is then selected; else anywhere in the inventory.  False when it had to be dropped at the player's feet.
	 */
	public static boolean toHand(ServerPlayer player, ItemStack stack) {
		Inventory inventory = player.getInventory();
		ItemStack held = inventory.getSelected();
		if (held.isEmpty()) {
			inventory.setItem(inventory.selected, stack);
			return true;
		}
		if (ItemStack.isSameItemSameTags(held, stack) && held.getCount() + stack.getCount() <= held.getMaxStackSize()) {
			held.grow(stack.getCount());
			return true;
		}
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			if (inventory.getItem(slot).isEmpty()) {
				inventory.setItem(slot, stack);
				inventory.selected = slot;
				player.connection.send(new ClientboundSetCarriedItemPacket(slot));
				return true;
			}
		}
		return give(player, stack);
	}

	/** Into the inventory as /give would; false when some of it had to be dropped at the player's feet. */
	public static boolean give(ServerPlayer player, ItemStack stack) {
		if (player.getInventory().add(stack) && stack.isEmpty()) return true;
		ItemEntity dropped = player.drop(stack, false);
		if (dropped != null) dropped.setNoPickUpDelay();
		return false;
	}

	/** Takes {@code count} empty maps, those in the hand first, so the hand is where the QR map goes. */
	public static void takeEmptyMaps(Inventory inventory, int count) {
		int left = take(inventory, inventory.selected, count);
		for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) left = take(inventory, slot, left);
	}

	private static int take(Inventory inventory, int slot, int count) {
		ItemStack stack = inventory.getItem(slot);
		if (count <= 0 || !stack.is(Items.MAP)) return count;
		int taken = Math.min(count, stack.getCount());
		stack.shrink(taken);
		return count - taken;
	}
}
