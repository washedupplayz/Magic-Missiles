package net.washedupplayz.magicmissiles.client;

import net.minecraft.resources.ResourceLocation;
import net.washedupplayz.magicmissiles.MagicMissiles;
import software.bernie.geckolib.model.DefaultedGeoModel;

/**
 * GeckoLib model for the missile ghost. Uses the {@code "entity"} subtype so it
 * resolves the very same assets as the missile entity did
 * ({@code geo/entity/missile.geo.json}, {@code textures/entity/missile.png},
 * {@code animations/entity/missile.animation.json}) — the ghost is visually
 * identical to the old rendered entity.
 */
public class MissileGhostModel extends DefaultedGeoModel<MissileGhost> {
    public MissileGhostModel() {
        super(ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, "missile"));
    }

    @Override
    protected String subtype() {
        return "entity";
    }
}
