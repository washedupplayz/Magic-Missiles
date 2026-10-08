package net.washedupplayz.magicmissiles.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.block.entity.ControlTerminalBlockEntity;
import net.washedupplayz.magicmissiles.block.entity.MissileDisplayBlockEntity;
import net.washedupplayz.magicmissiles.block.entity.RadarBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MagicMissiles.MOD_ID);

    public static final Supplier<BlockEntityType<RadarBlockEntity>> RADAR = BLOCK_ENTITIES.register(
            "radar",
            () -> BlockEntityType.Builder.of(RadarBlockEntity::new, ModBlocks.RADAR.get()).build(null));

    public static final Supplier<BlockEntityType<MissileDisplayBlockEntity>> MISSILE_DISPLAY =
            BLOCK_ENTITIES.register(
                    "missile_display",
                    () -> BlockEntityType.Builder.of(
                            MissileDisplayBlockEntity::new, ModBlocks.MISSILE_DISPLAY.get()).build(null));

    public static final Supplier<BlockEntityType<ControlTerminalBlockEntity>> CONTROL_TERMINAL =
            BLOCK_ENTITIES.register(
                    "control_terminal",
                    () -> BlockEntityType.Builder.of(
                            ControlTerminalBlockEntity::new, ModBlocks.CONTROL_TERMINAL.get()).build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
