package net.washedupplayz.magicmissiles.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.registry.ModBlockEntities;
import net.washedupplayz.magicmissiles.registry.ModEntities;

/**
 * Client-only setup: registers the GeckoLib renderers for the missile entity
 * and the radar block entity.
 */
@EventBusSubscriber(modid = MagicMissiles.MOD_ID, value = Dist.CLIENT)
public final class ModClientEvents {
    private ModClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MISSILE.get(), MissileRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RADAR.get(), RadarRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Game-bus (per-tick/world) listeners live on NeoForge.EVENT_BUS, registered here
        // from the mod-bus setup event so we stay on the client dist only.
        NeoForge.EVENT_BUS.addListener(ModClientGameEvents::onEntityJoinLevel);
    }
}
