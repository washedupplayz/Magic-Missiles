package net.washedupplayz.magicmissiles.terminal;

import java.util.List;
import java.util.Locale;

import net.minecraft.world.phys.Vec3;

public final class CommandLine {
    private CommandLine() {}

    public static List<String> tokenize(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return List.of(trimmed.toLowerCase(Locale.ROOT).split("\\s+"));
    }

    // "~" and "~n" are relative to base, like vanilla commands
    public static double coordinate(String token, double base) throws CommandException {
        if (token.startsWith("~")) {
            String offset = token.substring(1);
            return base + (offset.isEmpty() ? 0.0 : number(offset));
        }
        return number(token);
    }

    public static Vec3 position(List<String> tokens, int from, Vec3 base) throws CommandException {
        if (tokens.size() != from + 3) {
            throw new CommandException("expected <x> <y> <z>");
        }
        return new Vec3(
                coordinate(tokens.get(from), base.x),
                coordinate(tokens.get(from + 1), base.y),
                coordinate(tokens.get(from + 2), base.z));
    }

    public static int integer(String token, int min, int max) throws CommandException {
        int value;
        try {
            value = Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new CommandException("not a whole number: " + token);
        }
        if (value < min || value > max) {
            throw new CommandException(token + " is outside " + min + ".." + max);
        }
        return value;
    }

    private static double number(String token) throws CommandException {
        double value;
        try {
            value = Double.parseDouble(token);
        } catch (NumberFormatException e) {
            throw new CommandException("not a number: " + token);
        }
        // parseDouble accepts NaN and Infinity
        if (!Double.isFinite(value)) {
            throw new CommandException("not a number: " + token);
        }
        return value;
    }
}
