package net.washedupplayz.magicmissiles.block.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.registry.ModBlockEntities;
import net.washedupplayz.magicmissiles.terminal.TerminalCommands;

public class ControlTerminalBlockEntity extends BlockEntity {
    public static final int LOG_LINES = 64;
    private static final int TICKS_PER_SECOND = 20;

    private final List<String> log = new ArrayList<>();
    @Nullable
    private BlockPos silo;
    @Nullable
    private Vec3 target;
    // NaN for the default ceiling
    private double altitude = Double.NaN;
    // -1 when idle
    private int countdownTicks = -1;
    @Nullable
    private UUID launcher;
    // last search, picked from by link <n>, not saved
    private List<BlockPos> searchResults = List.of();

    public ControlTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROL_TERMINAL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ControlTerminalBlockEntity terminal) {
        if (terminal.countdownTicks < 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (terminal.countdownTicks == 0) {
            terminal.countdownTicks = -1;
            TerminalCommands.notifyLauncher(serverLevel, terminal, "ignition");
            TerminalCommands.ignite(serverLevel, terminal);
            terminal.launcher = null;
            terminal.sync();
            return;
        }
        if (terminal.countdownTicks % TICKS_PER_SECOND == 0) {
            String line = "T-" + terminal.secondsLeft();
            terminal.print(line);
            TerminalCommands.notifyLauncher(serverLevel, terminal, line);
        }
        terminal.countdownTicks--;
        terminal.setChanged();
    }

    public List<String> log() {
        return Collections.unmodifiableList(log);
    }

    public void print(String line) {
        log.add(line);
        while (log.size() > LOG_LINES) {
            log.remove(0);
        }
        sync();
    }

    public void clearLog() {
        log.clear();
        sync();
    }

    @Nullable
    public BlockPos silo() {
        return silo;
    }

    public void setSilo(@Nullable BlockPos silo) {
        this.silo = silo;
        sync();
    }

    public List<BlockPos> searchResults() {
        return searchResults;
    }

    public void setSearchResults(List<BlockPos> searchResults) {
        this.searchResults = List.copyOf(searchResults);
    }

    @Nullable
    public Vec3 target() {
        return target;
    }

    public void setTarget(@Nullable Vec3 target) {
        this.target = target;
        sync();
    }

    public double altitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
        sync();
    }

    public boolean countingDown() {
        return countdownTicks >= 0;
    }

    public int secondsLeft() {
        return (countdownTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    @Nullable
    public UUID launcher() {
        return launcher;
    }

    public void startCountdown(int seconds, UUID launcher) {
        this.countdownTicks = seconds * TICKS_PER_SECOND;
        this.launcher = launcher;
        sync();
    }

    public void stopCountdown() {
        this.countdownTicks = -1;
        this.launcher = null;
        sync();
    }

    // the screen reads everything from the client copy
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag lines = new ListTag();
        for (String line : log) {
            lines.add(StringTag.valueOf(line));
        }
        tag.put("Log", lines);
        if (silo != null) {
            tag.putLong("Silo", silo.asLong());
        }
        if (target != null) {
            tag.putDouble("TX", target.x);
            tag.putDouble("TY", target.y);
            tag.putDouble("TZ", target.z);
        }
        if (!Double.isNaN(altitude)) {
            tag.putDouble("Altitude", altitude);
        }
        tag.putInt("Countdown", countdownTicks);
        if (launcher != null) {
            tag.putUUID("Launcher", launcher);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        log.clear();
        ListTag lines = tag.getList("Log", Tag.TAG_STRING);
        for (int i = 0; i < lines.size(); i++) {
            log.add(lines.getString(i));
        }
        silo = tag.contains("Silo") ? BlockPos.of(tag.getLong("Silo")) : null;
        target = tag.contains("TX")
                ? new Vec3(tag.getDouble("TX"), tag.getDouble("TY"), tag.getDouble("TZ"))
                : null;
        altitude = tag.contains("Altitude") ? tag.getDouble("Altitude") : Double.NaN;
        countdownTicks = tag.contains("Countdown") ? tag.getInt("Countdown") : -1;
        launcher = tag.hasUUID("Launcher") ? tag.getUUID("Launcher") : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
