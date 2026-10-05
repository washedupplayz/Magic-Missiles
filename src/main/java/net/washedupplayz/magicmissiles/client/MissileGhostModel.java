package net.washedupplayz.magicmissiles.client;

import net.minecraft.resources.ResourceLocation;
import net.washedupplayz.magicmissiles.MagicMissiles;
import software.bernie.geckolib.model.DefaultedGeoModel;

public class MissileGhostModel extends DefaultedGeoModel<MissileGhost> {
    public MissileGhostModel() {
        super(ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, "missile"));
    }

    @Override
    protected String subtype() {
        return "entity";
    }
}
