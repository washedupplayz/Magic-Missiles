package net.washedupplayz.magicmissiles;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.washedupplayz.magicmissiles.missile.ModServerEvents;
import net.washedupplayz.magicmissiles.network.ModNetwork;
import net.washedupplayz.magicmissiles.registry.ModBlockEntities;
import net.washedupplayz.magicmissiles.registry.ModBlocks;
import net.washedupplayz.magicmissiles.registry.ModCreativeTabs;
import net.washedupplayz.magicmissiles.registry.ModItems;
import net.washedupplayz.magicmissiles.registry.ModSounds;
import org.slf4j.Logger;

/**
 * Magic Missiles — a tech/military mod centred on launchable missiles with
 * target tracking, radar, and missile interception.
 */
@Mod(MagicMissiles.MOD_ID)
public class MagicMissiles {
    public static final String MOD_ID = "magicmissiles";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MagicMissiles(IEventBus modBus, ModContainer modContainer) {
        LOGGER.info("Magic Missiles is arming up...");

        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModSounds.register(modBus);
        ModBlockEntities.register(modBus);
        ModCreativeTabs.register(modBus);

        // Networking (mod bus) and the per-level missile simulation (game bus).
        modBus.addListener(ModNetwork::register);
        NeoForge.EVENT_BUS.addListener(ModServerEvents::onLevelTick);
    }
}
