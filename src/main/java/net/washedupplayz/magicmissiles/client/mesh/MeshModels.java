package net.washedupplayz.magicmissiles.client.mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.washedupplayz.magicmissiles.MagicMissiles;

// not reachable from a blockstate or item, so side-loaded as standalone models
// radar.obj has base and dish groups, each wrapper json shows one via visibility
public final class MeshModels {
    public static final ModelResourceLocation MM1 = standalone("entity/mm1");
    public static final ModelResourceLocation RADAR_BASE = standalone("block/radar_base");
    public static final ModelResourceLocation RADAR_DISH = standalone("block/radar_dish");

    private MeshModels() {}

    private static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, path));
    }

    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(MM1);
        event.register(RADAR_BASE);
        event.register(RADAR_DISH);
    }

    public static BakedModel get(ModelResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(id);
    }
}
