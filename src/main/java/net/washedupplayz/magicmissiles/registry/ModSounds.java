package net.washedupplayz.magicmissiles.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.washedupplayz.magicmissiles.MagicMissiles;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MagicMissiles.MOD_ID);

    /** One-shot "startup" sound played when a missile launches. */
    public static final Supplier<SoundEvent> MISSILE_LAUNCH = register("missile_launch");

    /** One-shot sound played when a missile detonates. */
    public static final Supplier<SoundEvent> MISSILE_IMPACT = register("missile_impact");

    private ModSounds() {}

    private static Supplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, name)));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
