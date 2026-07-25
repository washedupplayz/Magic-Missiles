package net.washedupplayz.magicmissiles.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
}
