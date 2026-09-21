package net.washedupplayz.magicmissiles.registry;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.block.MissileDisplayBlock;
import net.washedupplayz.magicmissiles.block.MissileSiloBlock;
import net.washedupplayz.magicmissiles.block.RadarBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MagicMissiles.MOD_ID);

    /** Stationary launcher block: right-click to fire a missile skyward. */
    public static final DeferredBlock<MissileSiloBlock> MISSILE_SILO = registerBlock(
            "missile_silo",
            () -> new MissileSiloBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(4.0f)
                    .requiresCorrectToolForDrops()));

    /** Radar station: scans a radius and publishes tracked targets. */
    public static final DeferredBlock<RadarBlock> RADAR = registerBlock(
            "radar",
            () -> new RadarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()));

    /** Inspection stand: holds a missile mesh still so it can be looked at. */
    public static final DeferredBlock<MissileDisplayBlock> MISSILE_DISPLAY = registerBlock(
            "missile_display",
            () -> new MissileDisplayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0f)
                    .noOcclusion()));

    private ModBlocks() {}

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> supplier) {
        DeferredBlock<T> block = BLOCKS.register(name, supplier);
        ModItems.ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
