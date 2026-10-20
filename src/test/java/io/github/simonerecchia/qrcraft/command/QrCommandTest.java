package io.github.simonerecchia.qrcraft.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

/**
 * Every form of /qr is in the tree.  Brigadier merges a child into a sibling of the same name, so a literal called
 * "text" beside the {@code <text>} argument once took /qr &lt;text&gt; away without a word from the compiler.
 */
class QrCommandTest {
	@Test
	void everyFormOfTheCommandIsInTheTree() {
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		LiteralCommandNode<CommandSourceStack> qr = dispatcher.register(QrCommand.tree());
		// unrestricted, so no source is asked for its permission level
		Set<String> usage = Set.of(dispatcher.getAllUsage(qr, null, false));
		assertEquals(Set.of(
				"",
				"help",
				"<text>",
				"raw <text>",
				"color <code> <background> <text>",
				"poster <size> <text>",
				"poster <size> color <code> <background> <text>",
				"book",
				"book color <code> <background>"), usage);
	}
}
