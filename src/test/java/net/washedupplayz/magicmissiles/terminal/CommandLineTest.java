package net.washedupplayz.magicmissiles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CommandLineTest {
    private static final Vec3 PLAYER = new Vec3(100.5, 64.0, -20.25);

    @Test
    void tokenizeSplitsOnAnyWhitespaceAndLowercases() {
        assertEquals(List.of("target", "1", "2", "3"), CommandLine.tokenize("  Target 1\t2   3 "));
    }

    @Test
    void blankLineHasNoTokens() {
        assertTrue(CommandLine.tokenize("   ").isEmpty());
    }

    @Test
    void absoluteCoordinatesIgnoreTheBase() throws CommandException {
        assertEquals(new Vec3(10, -5, 2.5),
                CommandLine.position(List.of("target", "10", "-5", "2.5"), 1, PLAYER));
    }

    @Test
    void tildeIsRelativeToTheBase() throws CommandException {
        assertEquals(new Vec3(100.5, 74.0, -120.25),
                CommandLine.position(List.of("target", "~", "~10", "~-100"), 1, PLAYER));
    }

    @Test
    void positionNeedsExactlyThreeCoordinates() {
        assertThrows(CommandException.class,
                () -> CommandLine.position(List.of("target", "1", "2"), 1, PLAYER));
        assertThrows(CommandException.class,
                () -> CommandLine.position(List.of("target", "1", "2", "3", "4"), 1, PLAYER));
    }

    @Test
    void rejectsGarbageAndNonFiniteNumbers() {
        assertThrows(CommandException.class, () -> CommandLine.coordinate("abc", 0.0));
        assertThrows(CommandException.class, () -> CommandLine.coordinate("nan", 0.0));
        assertThrows(CommandException.class, () -> CommandLine.coordinate("~infinity", 0.0));
    }

    @Test
    void integerEnforcesItsRange() throws CommandException {
        assertEquals(30, CommandLine.integer("30", 0, 600));
        assertThrows(CommandException.class, () -> CommandLine.integer("601", 0, 600));
        assertThrows(CommandException.class, () -> CommandLine.integer("-1", 0, 600));
        assertThrows(CommandException.class, () -> CommandLine.integer("2.5", 0, 600));
    }
}
