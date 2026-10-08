package net.washedupplayz.magicmissiles.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class SiloRegistryTest {
    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static final BlockPos NEAR = new BlockPos(10, 64, 0);
    private static final BlockPos MID = new BlockPos(0, 64, -200);
    private static final BlockPos FAR = new BlockPos(3000, 70, 3000);

    @Test
    void sortsNearestFirst() {
        assertEquals(List.of(NEAR, MID, FAR),
                SiloRegistry.nearest(List.of(FAR, NEAR, MID), ORIGIN, Double.POSITIVE_INFINITY, 20));
    }

    @Test
    void radiusFiltersAndIsInclusive() {
        assertEquals(List.of(NEAR, MID),
                SiloRegistry.nearest(List.of(FAR, NEAR, MID), ORIGIN, 200.0, 20));
    }

    @Test
    void limitKeepsTheNearest() {
        assertEquals(List.of(NEAR),
                SiloRegistry.nearest(List.of(FAR, NEAR, MID), ORIGIN, Double.POSITIVE_INFINITY, 1));
    }

    @Test
    void emptyRegistryFindsNothing() {
        assertEquals(List.of(), SiloRegistry.nearest(List.of(), ORIGIN, Double.POSITIVE_INFINITY, 20));
    }
}
