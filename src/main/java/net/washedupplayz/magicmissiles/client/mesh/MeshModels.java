package net.washedupplayz.magicmissiles.client.mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.washedupplayz.magicmissiles.MagicMissiles;

/**
 * The OBJ mesh models our own renderers draw.
 *
 * <p>None of these are reachable from a blockstate or an item, so the model
 * manager would never load them on its own. They are side-loaded through
 * {@link ModelEvent.RegisterAdditional} as {@code standalone} variants and looked
 * up again by the same {@link ModelResourceLocation}.
 *
 * <p>The radar is one {@code radar.obj} with two groups, {@code base} and
 * {@code dish}. The two wrapper JSONs select one group each through the
 * root-level {@code visibility} map, which the OBJ loader honours via
 * {@code IGeometryBakingContext.isComponentVisible}. That gives us a separately
 * transformable dish without splitting the asset into two files.
 */
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

    /** Never null — a model that failed to load resolves to the missing-model placeholder. */
    public static BakedModel get(ModelResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(id);
    }
}
