package io.github.simonerecchia.qrcraft.command;

import io.github.simonerecchia.qrcraft.QrCraft;
import io.github.simonerecchia.qrcraft.core.Cooldowns;
import io.github.simonerecchia.qrcraft.core.Messages;
import io.github.simonerecchia.qrcraft.core.ServerConfig;
import io.github.simonerecchia.qrcraft.map.QrColors;
import io.github.simonerecchia.qrcraft.map.QrMaps;
import io.github.simonerecchia.qrcraft.qr.BookText;
import io.github.simonerecchia.qrcraft.qr.Contrast;
import io.github.simonerecchia.qrcraft.qr.Labels;
import io.github.simonerecchia.qrcraft.qr.MapKey;
import io.github.simonerecchia.qrcraft.qr.QrPicture;
import io.github.simonerecchia.qrcraft.qrcodegen.QrCode;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * {@code /qr}: a text, a poster or a book in your hand, as a QR code on maps.
 *
 * <pre>
 * /qr &lt;text&gt;                                  one map, in the player's hand
 * /qr poster &lt;size&gt; [color &lt;c&gt; &lt;b&gt;] &lt;text&gt;    size x size maps, into the inventory
 * /qr color &lt;code&gt; &lt;background&gt; &lt;text&gt;
 * /qr book [color &lt;c&gt; &lt;b&gt;]                     the smallest wall the book's text reads well on
 * /qr text &lt;text&gt;    /qr help
 * </pre>
 *
 * Brigadier takes a literal over the text argument, so a text starting with poster, color, book, text or help goes
 * through {@code /qr text}.  A command block reaches a player with {@code execute as}.
 */
public final class QrCommand {
	private static final Cooldowns COOLDOWNS = new Cooldowns();
	private static final SuggestionProvider<CommandSourceStack> COLORS =
			(context, builder) -> SharedSuggestionProvider.suggest(QrColors.names(), builder);
	private static final List<String> HELP = List.of("qrcraft.help.text", "qrcraft.help.poster", "qrcraft.help.color",
			"qrcraft.help.book", "qrcraft.help.literal");
	/** Code points of the text in a map's name, and the lore's preview of it: about what a tooltip shows unwrapped. */
	private static final int NAME_LENGTH = 32;
	private static final int LORE_WIDTH = 40;
	private static final int LORE_LINES = 3;

	private QrCommand() {
	}

	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(tree());
	}

	static LiteralArgumentBuilder<CommandSourceStack> tree() {
		return Commands.literal("qr")
				.requires(source -> source.hasPermission(ServerConfig.permissionLevel()))
				.executes(QrCommand::help)
				.then(Commands.literal("help").executes(QrCommand::help))
				.then(Commands.literal("text").then(text(false, false)))
				.then(Commands.literal("poster")
						.then(Commands.argument("size", IntegerArgumentType.integer(1, QrPicture.MAX_GRID))
								.then(colorThenText(true))
								.then(text(true, false))))
				.then(Commands.literal("book")
						.executes(context -> book(context, false))
						.then(Commands.literal("color").then(Commands.argument("code", StringArgumentType.word()).suggests(COLORS)
								.then(Commands.argument("background", StringArgumentType.word()).suggests(COLORS)
										.executes(context -> book(context, true))))))
				.then(colorThenText(false))
				.then(text(false, false));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> colorThenText(boolean poster) {
		return Commands.literal("color").then(Commands.argument("code", StringArgumentType.word()).suggests(COLORS)
				.then(Commands.argument("background", StringArgumentType.word()).suggests(COLORS)
						.then(text(poster, true))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> text(boolean poster, boolean colored) {
		return Commands.argument("text", StringArgumentType.greedyString())
				.executes(context -> create(context, poster ? IntegerArgumentType.getInteger(context, "size") : 1, colored,
						StringArgumentType.getString(context, "text"), null));
	}

	private static int help(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String language = Messages.language(source);
		MutableComponent help = Messages.line(language, "qrcraft.help.header").withStyle(ChatFormatting.GOLD);
		for (String key : HELP) help.append(Component.literal("\n")).append(Messages.line(language, key).withStyle(ChatFormatting.GRAY));
		source.sendSuccess(() -> help, false);
		return 1;
	}

	private static int book(CommandContext<CommandSourceStack> context, boolean colored) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String language = Messages.language(source);
		ItemStack book = heldBook(player);
		if (book == null) return fail(source, language, "qrcraft.error.no_book");
		String text = BookText.join(pages(book));
		if (text.isEmpty()) return fail(source, language, "qrcraft.error.empty_book");
		return create(context, 0, colored, text, title(book));
	}

	/**
	 * Draws {@code text} on a {@code grid}-sided wall, 0 for the smallest one it reads well on, and hands the maps over.
	 * {@code title}, when there is one, names the maps instead of the text's first line.
	 */
	private static int create(CommandContext<CommandSourceStack> context, int grid, boolean colored, String text, @Nullable String title)
			throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String language = Messages.language(source);
		if (text.isBlank()) return fail(source, language, "qrcraft.error.blank");

		DyeColor dark = QrColors.DARK;
		DyeColor light = QrColors.LIGHT;
		if (colored) {
			String darkName = StringArgumentType.getString(context, "code");
			String lightName = StringArgumentType.getString(context, "background");
			Optional<DyeColor> chosenDark = QrColors.byName(darkName);
			Optional<DyeColor> chosenLight = QrColors.byName(lightName);
			if (chosenDark.isEmpty()) return fail(source, language, "qrcraft.error.unknown_color", darkName);
			if (chosenLight.isEmpty()) return fail(source, language, "qrcraft.error.unknown_color", lightName);
			dark = chosenDark.get();
			light = chosenLight.get();
			if (dark == light) return fail(source, language, "qrcraft.error.same_color");
		}

		int maxGrid = ServerConfig.maxPosterSize();
		if (grid > maxGrid) return fail(source, language, "qrcraft.error.poster_size", maxGrid);

		MinecraftServer server = source.getServer();
		boolean exempt = source.hasPermission(2) || server.isSingleplayerOwner(player.getGameProfile());
		long now = System.currentTimeMillis();
		long wait = exempt ? 0 : COOLDOWNS.remaining(player.getUUID(), now, ServerConfig.cooldownSeconds() * 1000L);
		if (wait > 0) return fail(source, language, "qrcraft.error.cooldown", Cooldowns.seconds(wait));

		QrCode.Ecc ecc = ServerConfig.errorCorrection();
		Optional<QrPicture> encoded = grid == 0 ? QrPicture.fit(text, maxGrid, ecc) : QrPicture.encode(text, grid, ecc);
		if (encoded.isEmpty()) {
			int bytes = text.getBytes(StandardCharsets.UTF_8).length;
			int fits = QrPicture.smallestGrid(text, maxGrid, ecc);
			if (fits > 0) return fail(source, language, "qrcraft.error.too_long_single", bytes, fits);
			return fail(source, language, "qrcraft.error.too_long", maxGrid, bytes);
		}
		QrPicture picture = encoded.get();
		int side = picture.grid();
		int tiles = side * side;

		Inventory inventory = player.getInventory();
		boolean pays = ServerConfig.consumeEmptyMaps() && !player.getAbilities().instabuild;
		if (pays) {
			int have = inventory.countItem(Items.MAP);
			if (have < tiles) return fail(source, language, "qrcraft.error.cost", tiles, have);
		}

		byte darkByte = QrColors.packed(dark);
		byte lightByte = QrColors.packed(light);
		String key = ServerConfig.reuseIdenticalMaps() ? MapKey.of(text, side, ecc.name(), darkByte, lightByte) : null;
		int[] ids = QrMaps.draw(server, picture, darkByte, lightByte, key);
		if (pays) QrMaps.takeEmptyMaps(inventory, tiles);
		COOLDOWNS.mark(player.getUUID(), now);

		String shown = Labels.title(title != null ? title : text, NAME_LENGTH);
		List<Component> preview = new ArrayList<>();
		for (String line : Labels.preview(text, LORE_WIDTH, LORE_LINES)) preview.add(Messages.upright(line, ChatFormatting.GRAY));
		int markings = QrColors.rgb(dark);
		boolean kept = true;
		if (side == 1) {
			Component name = Messages.upright(Messages.text(language, "qrcraft.item.name", shown), ChatFormatting.WHITE);
			kept = QrMaps.toHand(player, QrMaps.item(ids[0], name, preview, markings, 0, 0, 1));
		} else {
			for (int row = 0; row < side; row++) {
				for (int column = 0; column < side; column++) {
					int number = row * side + column + 1;
					Component name = Messages.upright(Messages.text(language, "qrcraft.item.tile", number, tiles, shown), ChatFormatting.WHITE);
					List<Component> lore = new ArrayList<>();
					lore.add(Messages.upright(Messages.text(language, "qrcraft.item.position", row + 1, column + 1, side), ChatFormatting.YELLOW));
					lore.addAll(preview);
					kept &= QrMaps.give(player, QrMaps.item(ids[number - 1], name, lore, markings, row, column, side));
				}
			}
		}

		if (side == 1) {
			source.sendSuccess(() -> Messages.line(language, "qrcraft.created").withStyle(ChatFormatting.GREEN), false);
			if (picture.module() < QrPicture.COMFORTABLE_MODULE && maxGrid > 1) {
				source.sendSuccess(() -> Messages.line(language, "qrcraft.created.dense").withStyle(ChatFormatting.GRAY), false);
			}
		} else {
			source.sendSuccess(() -> Messages.line(language, "qrcraft.created.poster", side, tiles).withStyle(ChatFormatting.GREEN), false);
		}
		int darkRgb = QrColors.rgb(dark);
		int lightRgb = QrColors.rgb(light);
		if (Contrast.inverted(darkRgb, lightRgb)) {
			source.sendSuccess(() -> Messages.warning(language, "qrcraft.warn.inverted"), false);
		} else if (Contrast.ratio(darkRgb, lightRgb) < Contrast.MINIMUM) {
			source.sendSuccess(() -> Messages.warning(language, "qrcraft.warn.contrast"), false);
		}
		if (!kept) source.sendSuccess(() -> Messages.warning(language, "qrcraft.dropped"), false);

		QrCraft.LOGGER.info("{} made a {}x{} QR code, version {}, on map(s) {}", player.getGameProfile().getName(), side, side,
				picture.version(), Arrays.toString(ids));
		return tiles;
	}

	private static int fail(CommandSourceStack source, String language, String key, Object... args) {
		source.sendFailure(Messages.line(language, key, args));
		return 0;
	}

	@Nullable
	private static ItemStack heldBook(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK)) return stack;
		}
		return null;
	}

	/** A book and quill keeps its pages as plain text; a written book as text components in JSON. */
	private static List<String> pages(ItemStack book) {
		CompoundTag tag = book.getTag();
		if (tag == null) return List.of();
		ListTag pages = tag.getList("pages", Tag.TAG_STRING);
		boolean written = book.is(Items.WRITTEN_BOOK);
		List<String> out = new ArrayList<>();
		for (int i = 0; i < pages.size(); i++) out.add(written ? plain(pages.getString(i)) : pages.getString(i));
		return out;
	}

	/** A malformed page is taken as it stands rather than failing the whole book. */
	private static String plain(String json) {
		try {
			Component page = Component.Serializer.fromJson(json);
			return page == null ? json : page.getString();
		} catch (RuntimeException e) {
			return json;
		}
	}

	@Nullable
	private static String title(ItemStack book) {
		CompoundTag tag = book.getTag();
		if (tag == null || !book.is(Items.WRITTEN_BOOK)) return null;
		String title = tag.getString("title");
		return title.isBlank() ? null : title;
	}
}
