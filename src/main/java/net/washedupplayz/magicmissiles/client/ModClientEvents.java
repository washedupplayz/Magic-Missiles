package net.washedupplayz.magicmissiles.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.registry.ModEntities;

/**
 * Client-only setup. The missile renders as its item icon via the vanilla
 * {@link ThrownItemRenderer}, so no custom model is needed yet.
 */
@EventBusSubscriber(modid = MagicMissiles.MOD_ID, value = Dist.CLIENT)
public final class ModClientEvents {
    private ModClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MISSILE.get(), ThrownItemRenderer::new);
    }
}
