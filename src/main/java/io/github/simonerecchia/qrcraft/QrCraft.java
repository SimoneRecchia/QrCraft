package io.github.simonerecchia.qrcraft;

import io.github.simonerecchia.qrcraft.command.QrCommand;
import io.github.simonerecchia.qrcraft.core.ServerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * The mod's entry point.  QrCraft registers nothing - no block, no item, no packet - so it runs on the server alone
 * and a vanilla client joins as it would anywhere: the command makes ordinary locked filled maps.  mods.toml says so
 * with displayTest IGNORE_SERVER_VERSION; keep it that way, or every player has to install the mod.
 */
@Mod(QrCraft.MOD_ID)
public final class QrCraft {
	public static final String MOD_ID = "qrcraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public QrCraft() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		// stored with the world, not in config/: <world>/serverconfig/qrcraft-server.toml
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
		modBus.addListener(ServerConfig::onReload);

		MinecraftForge.EVENT_BUS.addListener(QrCommand::register);
	}
}
