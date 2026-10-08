package net.washedupplayz.magicmissiles.terminal;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.block.MissileSiloBlock;
import net.washedupplayz.magicmissiles.block.entity.ControlTerminalBlockEntity;
import net.washedupplayz.magicmissiles.missile.MissileManager;
import net.washedupplayz.magicmissiles.missile.MissileSpec;
import net.washedupplayz.magicmissiles.missile.MissileSpecs;
import net.washedupplayz.magicmissiles.missile.SiloRegistry;
import net.washedupplayz.magicmissiles.network.TerminalCommandPayload;
import net.washedupplayz.magicmissiles.registry.ModBlocks;

public final class TerminalCommands {
    public static final int DEFAULT_COUNTDOWN_SECONDS = 10;
    private static final int MAX_COUNTDOWN_SECONDS = 600;
    private static final double MAX_USE_DISTANCE_SQR = 8.0 * 8.0;
    // headroom above build height for the cruise altitude
    private static final double MAX_ALTITUDE_MARGIN = 64.0;
    private static final MissileSpec SPEC = MissileSpecs.STANDARD;
    private static final int SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_RADIUS = 30_000_000;

    private static final List<String> HELP = List.of(
            "help - this list",
            "status - link, target, altitude, countdown",
            "search [radius] - known silos, nearest first",
            "link <n> - silo number n from the last search",
            "link <x> <y> <z> - silo at a position",
            "unlink",
            "target <x> <y> <z> - point to fly to",
            "target clear",
            "altitude <y> - cruise altitude for launches",
            "altitude clear - default ceiling",
            "launch [seconds] - ignition countdown, default " + DEFAULT_COUNTDOWN_SECONDS,
            "abort - cancel the countdown",
            "clear - clear the screen",
            "~ and ~n are relative to you");

    private TerminalCommands() {}

    public static void handle(TerminalCommandPayload payload, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = payload.pos();
        if (!level.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_USE_DISTANCE_SQR) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof ControlTerminalBlockEntity terminal) {
            execute(level, terminal, player, StringUtil.filterText(payload.line()));
        }
    }

    public static void execute(ServerLevel level, ControlTerminalBlockEntity terminal,
                               ServerPlayer player, String line) {
        List<String> tokens = CommandLine.tokenize(line);
        if (tokens.isEmpty()) {
            return;
        }
        terminal.print("> " + line.trim());
        try {
            switch (tokens.get(0)) {
                case "help" -> HELP.forEach(terminal::print);
                case "status" -> status(level, terminal);
                case "search" -> search(level, terminal, tokens);
                case "link" -> link(level, terminal, player, tokens);
                case "unlink" -> {
                    terminal.setSilo(null);
                    terminal.print("unlinked");
                }
                case "target" -> target(terminal, player, tokens);
                case "altitude" -> altitude(level, terminal, player, tokens);
                case "launch" -> launch(level, terminal, player, tokens);
                case "abort" -> abort(terminal);
                case "clear" -> terminal.clearLog();
                default -> throw new CommandException("unknown command " + tokens.get(0) + ", try help");
            }
        } catch (CommandException e) {
            terminal.print("error: " + e.getMessage());
        }
    }

    // end of the countdown
    public static void ignite(ServerLevel level, ControlTerminalBlockEntity terminal) {
        try {
            BlockPos silo = requireSilo(level, terminal);
            Vec3 target = requireTarget(terminal);
            UUID launcher = terminal.launcher();
            ServerPlayer owner = launcher == null ? null : level.getServer().getPlayerList().getPlayer(launcher);
            long id = MissileManager.get(level).launchAt(MissileSiloBlock.launchPoint(silo),
                    MissileSiloBlock.LAUNCH_DIRECTION, SPEC, owner, target, terminal.altitude());
            terminal.print("ignition, missile " + id + " away");
        } catch (CommandException e) {
            terminal.print("launch failed: " + e.getMessage());
        }
    }

    private static void status(ServerLevel level, ControlTerminalBlockEntity terminal) {
        BlockPos silo = terminal.silo();
        Vec3 target = terminal.target();
        terminal.print("silo: " + (silo == null ? "not linked" : format(silo)));
        terminal.print("target: " + (target == null ? "none" : format(target)));
        terminal.print("altitude: " + (Double.isNaN(terminal.altitude())
                ? "default, y " + format(MissileManager.get(level).defaultCeiling())
                : "y " + format(terminal.altitude())));
        terminal.print("countdown: " + (terminal.countingDown()
                ? "T-" + terminal.secondsLeft() : "idle"));
        if (silo != null && target != null) {
            terminal.print(flightSummary(silo, target));
        }
    }

    private static void search(ServerLevel level, ControlTerminalBlockEntity terminal,
                               List<String> tokens) throws CommandException {
        if (tokens.size() > 2) {
            throw new CommandException("expected search [radius]");
        }
        double radius = tokens.size() == 2
                ? CommandLine.integer(tokens.get(1), 1, MAX_SEARCH_RADIUS)
                : Double.POSITIVE_INFINITY;
        BlockPos origin = terminal.getBlockPos();
        List<BlockPos> found = SiloRegistry.get(level).nearest(origin, radius, SEARCH_LIMIT);
        terminal.setSearchResults(found);
        if (found.isEmpty()) {
            terminal.print("no silos found");
            terminal.print("silos are known once placed, fired or linked by position");
            return;
        }
        for (int i = 0; i < found.size(); i++) {
            BlockPos silo = found.get(i);
            terminal.print((i + 1) + ": " + format(silo) + ", "
                    + format(Math.sqrt(silo.distSqr(origin))) + " blocks"
                    + (silo.equals(terminal.silo()) ? ", linked" : ""));
        }
        if (found.size() == SEARCH_LIMIT) {
            terminal.print("nearest " + SEARCH_LIMIT + " shown, narrow with search <radius>");
        }
        terminal.print("link <n> to choose");
    }

    private static void link(ServerLevel level, ControlTerminalBlockEntity terminal,
                             ServerPlayer player, List<String> tokens) throws CommandException {
        BlockPos silo;
        if (tokens.size() == 2) {
            List<BlockPos> results = terminal.searchResults();
            if (results.isEmpty()) {
                throw new CommandException("no search results, run search first");
            }
            silo = results.get(CommandLine.integer(tokens.get(1), 1, results.size()) - 1);
        } else if (tokens.size() == 4) {
            silo = BlockPos.containing(CommandLine.position(tokens, 1, player.position()));
        } else {
            throw new CommandException("expected link <n> or link <x> <y> <z>");
        }
        checkSilo(level, silo);
        terminal.setSilo(silo);
        terminal.print("linked silo at " + format(silo) + ", "
                + format(Math.sqrt(silo.distSqr(terminal.getBlockPos()))) + " blocks away");
    }

    private static void target(ControlTerminalBlockEntity terminal, ServerPlayer player,
                               List<String> tokens) throws CommandException {
        if (tokens.size() == 2 && tokens.get(1).equals("clear")) {
            terminal.setTarget(null);
            terminal.print("target cleared");
            return;
        }
        Vec3 target = CommandLine.position(tokens, 1, player.position());
        terminal.setTarget(target);
        terminal.print("target " + format(target));
        if (terminal.silo() != null) {
            terminal.print(flightSummary(terminal.silo(), target));
        }
    }

    private static void altitude(ServerLevel level, ControlTerminalBlockEntity terminal,
                                 ServerPlayer player, List<String> tokens) throws CommandException {
        if (tokens.size() != 2) {
            throw new CommandException("expected altitude <y> or altitude clear");
        }
        if (tokens.get(1).equals("clear")) {
            terminal.setAltitude(Double.NaN);
            terminal.print("altitude: default, y " + format(MissileManager.get(level).defaultCeiling()));
            return;
        }
        double y = CommandLine.coordinate(tokens.get(1), player.getY());
        double min = level.getMinBuildHeight();
        double max = level.getMaxBuildHeight() + MAX_ALTITUDE_MARGIN;
        if (y < min || y > max) {
            throw new CommandException("altitude must be within " + format(min) + ".." + format(max));
        }
        terminal.setAltitude(y);
        terminal.print("altitude y " + format(y) + ", terrain in loaded chunks is still solid");
    }

    private static void launch(ServerLevel level, ControlTerminalBlockEntity terminal,
                               ServerPlayer player, List<String> tokens) throws CommandException {
        if (terminal.countingDown()) {
            throw new CommandException("countdown already running, abort first");
        }
        if (tokens.size() > 2) {
            throw new CommandException("expected launch [seconds]");
        }
        int seconds = tokens.size() == 2
                ? CommandLine.integer(tokens.get(1), 0, MAX_COUNTDOWN_SECONDS)
                : DEFAULT_COUNTDOWN_SECONDS;
        requireSilo(level, terminal);
        requireTarget(terminal);
        terminal.startCountdown(seconds, player.getUUID());
        terminal.print("countdown started, ignition in " + seconds + " s");
    }

    private static void abort(ControlTerminalBlockEntity terminal) throws CommandException {
        if (!terminal.countingDown()) {
            throw new CommandException("no countdown running");
        }
        terminal.stopCountdown();
        terminal.print("aborted");
    }

    public static void notifyLauncher(ServerLevel level, ControlTerminalBlockEntity terminal, String text) {
        UUID launcher = terminal.launcher();
        ServerPlayer player = launcher == null ? null : level.getServer().getPlayerList().getPlayer(launcher);
        if (player != null) {
            player.displayClientMessage(Component.literal(text), true);
        }
    }

    private static BlockPos requireSilo(ServerLevel level, ControlTerminalBlockEntity terminal)
            throws CommandException {
        BlockPos silo = terminal.silo();
        if (silo == null) {
            throw new CommandException("no silo linked, use link <x> <y> <z>");
        }
        checkSilo(level, silo);
        return silo;
    }

    private static Vec3 requireTarget(ControlTerminalBlockEntity terminal) throws CommandException {
        Vec3 target = terminal.target();
        if (target == null) {
            throw new CommandException("no target, use target <x> <y> <z>");
        }
        return target;
    }

    // loads the silo chunk if needed, rare enough to do synchronously
    // also keeps the registry honest in both directions
    private static void checkSilo(ServerLevel level, BlockPos silo) throws CommandException {
        if (!level.isInWorldBounds(silo)) {
            throw new CommandException(format(silo) + " is outside the world");
        }
        SiloRegistry registry = SiloRegistry.get(level);
        if (!level.getBlockState(silo).is(ModBlocks.MISSILE_SILO.get())) {
            registry.remove(silo);
            throw new CommandException("no silo at " + format(silo));
        }
        registry.add(silo);
    }

    // ignores the climb, so a lower bound
    private static String flightSummary(BlockPos silo, Vec3 target) {
        double distance = MissileSiloBlock.launchPoint(silo).distanceTo(target);
        double seconds = distance / SPEC.cruiseSpeed() / 20.0;
        return "silo to target " + format(distance) + " blocks, at least " + format(seconds) + " s of flight";
    }

    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static String format(Vec3 v) {
        return format(v.x) + " " + format(v.y) + " " + format(v.z);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
