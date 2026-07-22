package net.washedupplayz.magicmissiles;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.washedupplayz.magicmissiles.registry.ModBlockEntities;
import net.washedupplayz.magicmissiles.registry.ModBlocks;
import net.washedupplayz.magicmissiles.registry.ModCreativeTabs;
import net.washedupplayz.magicmissiles.registry.ModEntities;
import net.washedupplayz.magicmissiles.registry.ModItems;
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
        ModBlockEntities.register(modBus);
        ModEntities.register(modBus);
        ModCreativeTabs.register(modBus);
    }
}
