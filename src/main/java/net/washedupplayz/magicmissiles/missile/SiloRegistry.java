package net.washedupplayz.magicmissiles.missile;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.washedupplayz.magicmissiles.MagicMissiles;

// silo positions per level, so search works across unloaded chunks
public class SiloRegistry extends SavedData {
    private static final String DATA_NAME = MagicMissiles.MOD_ID + "_silos";

    private final Set<Long> silos = new LinkedHashSet<>();

    public SiloRegistry() {}

    public static SiloRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SiloRegistry::new, (tag, provider) -> load(tag)), DATA_NAME);
    }

    private static SiloRegistry load(CompoundTag tag) {
        SiloRegistry registry = new SiloRegistry();
        for (long pos : tag.getLongArray("Silos")) {
            registry.silos.add(pos);
        }
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLongArray("Silos", silos.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    public void add(BlockPos pos) {
        if (silos.add(pos.asLong())) {
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (silos.remove(pos.asLong())) {
            setDirty();
        }
    }

    public List<BlockPos> nearest(BlockPos origin, double radius, int limit) {
        return nearest(silos.stream().map(BlockPos::of).toList(), origin, radius, limit);
    }

    public static List<BlockPos> nearest(Collection<BlockPos> silos, BlockPos origin, double radius, int limit) {
        double radiusSqr = radius * radius;
        return silos.stream()
                .filter(pos -> pos.distSqr(origin) <= radiusSqr)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(origin)))
                .limit(limit)
                .toList();
    }
}
