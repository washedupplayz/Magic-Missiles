package net.washedupplayz.magicmissiles.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.item.MissileLauncherItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagicMissiles.MOD_ID);

    /** Ammunition consumed by launchers. Rendered from the same mesh as the projectile. */
    public static final DeferredItem<Item> MM1 = ITEMS.registerSimpleItem("mm1");

    /** Handheld launcher for quick field-testing of missiles. */
    public static final DeferredItem<MissileLauncherItem> MISSILE_LAUNCHER = ITEMS.register(
            "missile_launcher",
            () -> new MissileLauncherItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
