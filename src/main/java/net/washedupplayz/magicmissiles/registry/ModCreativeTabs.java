package net.washedupplayz.magicmissiles.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.washedupplayz.magicmissiles.MagicMissiles;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicMissiles.MOD_ID);

    public static final Supplier<CreativeModeTab> MAGIC_MISSILES = CREATIVE_MODE_TABS.register(
            "magic_missiles",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MagicMissiles.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.MISSILE_LAUNCHER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.MM1.get());
                        output.accept(ModItems.MISSILE_LAUNCHER.get());
                        output.accept(ModBlocks.MISSILE_SILO.get());
                        output.accept(ModBlocks.RADAR.get());
                        output.accept(ModBlocks.MISSILE_DISPLAY.get());
                        output.accept(ModBlocks.CONTROL_TERMINAL.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}
