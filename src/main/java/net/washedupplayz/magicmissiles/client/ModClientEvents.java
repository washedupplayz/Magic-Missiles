package net.washedupplayz.magicmissiles.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.client.mesh.MeshModels;
import net.washedupplayz.magicmissiles.registry.ModBlockEntities;

@EventBusSubscriber(modid = MagicMissiles.MOD_ID, value = Dist.CLIENT)
public final class ModClientEvents {
    private ModClientEvents() {}

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        MeshModels.registerAdditional(event);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RADAR.get(), RadarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MISSILE_DISPLAY.get(), MissileDisplayRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // game bus listeners, registered here to stay client only
        NeoForge.EVENT_BUS.addListener(ClientMissileManager::onClientTick);
        NeoForge.EVENT_BUS.addListener(GhostRenderer::onRenderLevelStage);
    }
}
