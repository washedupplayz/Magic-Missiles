package net.washedupplayz.magicmissiles.registry;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.block.ControlTerminalBlock;
import net.washedupplayz.magicmissiles.block.MissileDisplayBlock;
import net.washedupplayz.magicmissiles.block.MissileSiloBlock;
import net.washedupplayz.magicmissiles.block.RadarBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MagicMissiles.MOD_ID);

    public static final DeferredBlock<MissileSiloBlock> MISSILE_SILO = registerBlock(
            "missile_silo",
            () -> new MissileSiloBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(4.0f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<RadarBlock> RADAR = registerBlock(
            "radar",
            () -> new RadarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    // without this the block occludes light, so the renderer samples
                    // light 0 at its own position and the dish draws pitch black
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<MissileDisplayBlock> MISSILE_DISPLAY = registerBlock(
            "missile_display",
            () -> new MissileDisplayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0f)
                    .noOcclusion()));

    public static final DeferredBlock<ControlTerminalBlock> CONTROL_TERMINAL = registerBlock(
            "control_terminal",
            () -> new ControlTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)));

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
