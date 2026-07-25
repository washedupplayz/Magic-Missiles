package net.washedupplayz.magicmissiles.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.block.entity.RadarBlockEntity;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Renders the radar block with its GeckoLib model, driving the spinning dish
 * animation. Resolves to {@code geo/block/radar.geo.json}, {@code textures/block/radar.png}
 * and {@code animations/block/radar.animation.json}.
 */
public class RadarRenderer extends GeoBlockRenderer<RadarBlockEntity> {
    public RadarRenderer(BlockEntityRendererProvider.Context context) {
        super(new DefaultedBlockGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, "radar")));
    }
}
