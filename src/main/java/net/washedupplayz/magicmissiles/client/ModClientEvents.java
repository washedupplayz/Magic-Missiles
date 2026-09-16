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

/**
 * Client-only setup: side-loads the OBJ meshes, registers the radar block-entity
 * renderer and wires the missile ghost's client-side tick/render listeners.
 * (Missiles are drawn by {@link GhostRenderer}, not an entity renderer.)
 */
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
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Game-bus (per-tick/world) listeners live on NeoForge.EVENT_BUS, registered here
        // from the mod-bus setup event so we stay on the client dist only.
        // Missile ghosts: advance them each client tick and draw them at long range.
        NeoForge.EVENT_BUS.addListener(ClientMissileManager::onClientTick);
        NeoForge.EVENT_BUS.addListener(GhostRenderer::onRenderLevelStage);
    }
}
