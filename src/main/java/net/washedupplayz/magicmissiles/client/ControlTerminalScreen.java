package net.washedupplayz.magicmissiles.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.washedupplayz.magicmissiles.block.entity.ControlTerminalBlockEntity;
import net.washedupplayz.magicmissiles.network.TerminalCommandPayload;

public class ControlTerminalScreen extends Screen {
    private static final int MAX_WIDTH = 380;
    private static final int MAX_HEIGHT = 240;
    private static final int PADDING = 8;
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private static final int BEZEL = 0xFF2B2E31;
    private static final int SCREEN = 0xF00A100A;
    private static final int TEXT = 0xFF33FF66;
    private static final int DIM = 0xFF1E8F3C;

    // shared across openings, like a shell history
    private static final List<String> HISTORY = new ArrayList<>();

    private final BlockPos pos;
    private EditBox input;
    private int historyIndex;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public ControlTerminalScreen(BlockPos pos) {
        super(Component.translatable("block.magicmissiles.control_terminal"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_WIDTH, width - 20);
        panelHeight = Math.min(MAX_HEIGHT, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;

        int promptWidth = font.width("> ");
        input = new EditBox(font, left + PADDING + promptWidth, top + panelHeight - PADDING - font.lineHeight,
                panelWidth - 2 * PADDING - promptWidth, font.lineHeight, Component.empty());
        input.setMaxLength(TerminalCommandPayload.MAX_LENGTH);
        input.setBordered(false);
        input.setTextColor(TEXT);
        addRenderableWidget(input);
        setInitialFocus(input);
        historyIndex = HISTORY.size();
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (terminal() == null || minecraft.player == null
                || minecraft.player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                submit();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                recall(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                recall(1);
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    private void submit() {
        String line = input.getValue().trim();
        if (line.isEmpty()) {
            return;
        }
        if (HISTORY.isEmpty() || !HISTORY.get(HISTORY.size() - 1).equals(line)) {
            HISTORY.add(line);
        }
        historyIndex = HISTORY.size();
        input.setValue("");
        PacketDistributor.sendToServer(new TerminalCommandPayload(pos, line));
    }

    private void recall(int step) {
        if (HISTORY.isEmpty()) {
            return;
        }
        historyIndex = Math.max(0, Math.min(HISTORY.size(), historyIndex + step));
        input.setValue(historyIndex == HISTORY.size() ? "" : HISTORY.get(historyIndex));
        input.moveCursorToEnd(false);
    }

    // no blur, the sky stays visible around the panel
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left - 3, top - 3, left + panelWidth + 3, top + panelHeight + 3, BEZEL);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, SCREEN);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ControlTerminalBlockEntity terminal = terminal();
        if (terminal == null) {
            return;
        }

        int textLeft = left + PADDING;
        int textWidth = panelWidth - 2 * PADDING;
        int lineHeight = font.lineHeight + 1;

        graphics.drawString(font, font.plainSubstrByWidth(statusLine(terminal), textWidth),
                textLeft, top + PADDING, DIM, false);
        int logTop = top + PADDING + lineHeight + 4;
        graphics.fill(textLeft, logTop - 3, textLeft + textWidth, logTop - 2, DIM);

        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String line : terminal.log()) {
            lines.addAll(font.split(Component.literal(line), textWidth));
        }
        int inputTop = top + panelHeight - PADDING - font.lineHeight;
        int y = inputTop - lineHeight - 2;
        for (int i = lines.size() - 1; i >= 0 && y >= logTop; i--) {
            graphics.drawString(font, lines.get(i), textLeft, y, TEXT, false);
            y -= lineHeight;
        }
        graphics.drawString(font, "> ", textLeft, inputTop, TEXT, false);
    }

    private static String statusLine(ControlTerminalBlockEntity terminal) {
        BlockPos silo = terminal.silo();
        Vec3 target = terminal.target();
        String altitude = Double.isNaN(terminal.altitude())
                ? "default" : String.format(Locale.ROOT, "%.0f", terminal.altitude());
        return "silo " + (silo == null ? "-" : silo.getX() + " " + silo.getY() + " " + silo.getZ())
                + "  target " + (target == null ? "-" : String.format(Locale.ROOT, "%.0f %.0f %.0f",
                        target.x, target.y, target.z))
                + "  alt " + altitude
                + "  " + (terminal.countingDown() ? "T-" + terminal.secondsLeft() : "idle");
    }

    private ControlTerminalBlockEntity terminal() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(pos) instanceof ControlTerminalBlockEntity terminal ? terminal : null;
    }

    @Override
    public boolean isPauseScreen() {
        // a pause screen would stop the countdown in singleplayer
        return false;
    }
}
