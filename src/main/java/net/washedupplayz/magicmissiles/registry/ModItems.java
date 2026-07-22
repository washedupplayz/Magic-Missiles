package net.washedupplayz.magicmissiles.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.item.MissileLauncherItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagicMissiles.MOD_ID);

    /** Ammunition consumed by launchers; also the flying projectile's icon. */
    public static final DeferredItem<Item> MISSILE = ITEMS.registerSimpleItem("missile");

    /** Handheld launcher for quick field-testing of missiles. */
    public static final DeferredItem<MissileLauncherItem> MISSILE_LAUNCHER = ITEMS.register(
            "missile_launcher",
            () -> new MissileLauncherItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
