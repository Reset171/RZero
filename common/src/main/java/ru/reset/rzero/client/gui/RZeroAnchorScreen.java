package ru.reset.rzero.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import ru.reset.rzero.anchor.AnchorMode;
import ru.reset.rzero.anchor.RZeroAnchorSettings;
import ru.reset.rzero.network.UpdateAnchorSettingsPacket;
import ru.reset.rzero.platform.Services;
import ru.reset.rzero.runtime.RZeroRuntime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RZeroAnchorScreen extends Screen {

    private AnchorMode selectedMode;
    private int rotationSeconds;
    private int cooldownSeconds;
    private final Set<UUID> selectedPlayers = new HashSet<>();

    private PlayerSelectionList playerList;
    private CycleButton<AnchorMode> modeButton;
    private EditBox cooldownBox;
    private EditBox rotationBox;
    private Button applyButton;

    public RZeroAnchorScreen() {
        super(Component.translatable("gui.rzero.anchor.title"));
        RZeroAnchorSettings current = RZeroRuntime.anchorSettings();
        this.selectedMode = current.mode();
        this.rotationSeconds = current.rotationSeconds();
        this.cooldownSeconds = current.rollbackCooldownSeconds();
        this.selectedPlayers.addAll(current.pinned());
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        this.modeButton = this.addRenderableWidget(
                CycleButton.<AnchorMode>builder(m -> Component.translatable("gui.rzero.anchor.mode." + m.id()))
                        .withValues(AnchorMode.values())
                        .withInitialValue(this.selectedMode)
                        .create(centerX - 155, 38, 150, 20, Component.translatable("gui.rzero.anchor.mode"),
                                (button, value) -> {
                                    this.selectedMode = value;
                                    updateWidgetStates();
                                })
        );

        this.cooldownBox = new EditBox(this.font, centerX + 5, 38, 150, 20, Component.translatable("gui.rzero.anchor.cooldown"));
        this.cooldownBox.setMaxLength(6);
        this.cooldownBox.setFilter(s -> s.matches("\\d*"));
        this.cooldownBox.setValue(String.valueOf(this.cooldownSeconds));
        this.cooldownBox.setHint(Component.translatable("gui.rzero.anchor.cooldown.disabled"));
        this.addRenderableWidget(this.cooldownBox);

        this.rotationBox = new EditBox(this.font, centerX - 155, 75, 310, 20, Component.translatable("gui.rzero.anchor.rotation"));
        this.rotationBox.setMaxLength(6);
        this.rotationBox.setFilter(s -> s.matches("\\d*"));
        this.rotationBox.setValue(String.valueOf(this.rotationSeconds));
        this.addRenderableWidget(this.rotationBox);

        int listTop = (this.selectedMode == AnchorMode.ROTATING) ? 102 : 66;
        int listBottom = this.height - 38;
        this.playerList = this.addRenderableWidget(
                new PlayerSelectionList(this.minecraft, this.width, listBottom - listTop, listTop, 24)
        );

        this.applyButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.rzero.anchor.apply"), b -> applySettings())
                        .bounds(centerX - 155, this.height - 30, 150, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                        .bounds(centerX + 5, this.height - 30, 150, 20)
                        .build()
        );

        updateWidgetStates();
        populatePlayers();
    }

    private void updateWidgetStates() {
        boolean isRotating = this.selectedMode == AnchorMode.ROTATING;
        this.rotationBox.visible = isRotating;
        this.rotationBox.setEditable(isRotating);

        boolean allowPlayerSelection = this.selectedMode == AnchorMode.FIXED || this.selectedMode == AnchorMode.MULTI;
        if (this.playerList != null) {
            this.playerList.active = allowPlayerSelection;
            int listTop = isRotating ? 102 : 66;
            int listBottom = this.height - 38;
            this.playerList.setRectangle(this.width, listBottom - listTop, 0, listTop);
        }
    }

    private void populatePlayers() {
        if (this.minecraft == null || this.minecraft.getConnection() == null) return;
        this.playerList.clearEntries();

        Collection<PlayerInfo> players = this.minecraft.getConnection().getOnlinePlayers();
        List<PlayerInfo> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparing(p -> p.getProfile().getName().toLowerCase()));

        for (PlayerInfo info : sorted) {
            this.playerList.addPlayerEntry(new PlayerListEntry(info));
        }
    }

    private void applySettings() {
        int cooldown = 0;
        try {
            String cdText = this.cooldownBox.getValue().trim();
            if (!cdText.isEmpty()) {
                cooldown = Math.max(0, Integer.parseInt(cdText));
            }
        } catch (NumberFormatException ignored) {}

        int rotation = RZeroAnchorSettings.MIN_ROTATION_SECONDS;
        try {
            String rotText = this.rotationBox.getValue().trim();
            if (!rotText.isEmpty()) {
                rotation = Math.max(RZeroAnchorSettings.MIN_ROTATION_SECONDS, Integer.parseInt(rotText));
            }
        } catch (NumberFormatException ignored) {}

        List<UUID> pinned = new ArrayList<>(this.selectedPlayers);
        RZeroAnchorSettings updated = new RZeroAnchorSettings(
                this.selectedMode,
                rotation,
                cooldown,
                pinned
        );
        Services.PLATFORM.sendToServer(new UpdateAnchorSettingsPacket(updated));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        int centerX = this.width / 2;
        graphics.drawString(this.font, Component.translatable("gui.rzero.anchor.cooldown.label"), centerX + 5, 26, 0xAAAAAA, false);
        if (this.selectedMode == AnchorMode.ROTATING) {
            graphics.drawString(this.font, Component.translatable("gui.rzero.anchor.rotation.label"), centerX - 155, 63, 0xAAAAAA, false);
        }

        if (this.selectedMode == AnchorMode.EVERYONE) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.rzero.anchor.note.everyone"),
                    this.width / 2, this.height / 2, 0xAAAAAA);
        } else if (this.selectedMode == AnchorMode.ROTATING) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.rzero.anchor.note.rotating"),
                    this.width / 2, this.height / 2 + 15, 0xAAAAAA);
        }
    }

    private class PlayerSelectionList extends ObjectSelectionList<PlayerListEntry> {

        public boolean active = true;

        public PlayerSelectionList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        public void addPlayerEntry(PlayerListEntry entry) {
            this.addEntry(entry);
        }

        public void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 145;
        }
    }

    private class PlayerListEntry extends ObjectSelectionList.Entry<PlayerListEntry> {
        private final PlayerInfo info;

        public PlayerListEntry(PlayerInfo info) {
            this.info = info;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.info.getProfile().getName());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean isHovered, float partialTick) {
            UUID uuid = this.info.getProfile().getId();
            boolean isSelected = selectedPlayers.contains(uuid);

            int alpha = playerList.active ? 0xFFFFFFFF : 0x77AAAAAA;

            PlayerFaceRenderer.draw(graphics, this.info.getSkin().texture(), left + 2, top + 2, 18);
            graphics.drawString(font, this.info.getProfile().getName(), left + 26, top + 6, alpha, false);

            if (playerList.active) {
                String mark = isSelected ? "[X]" : "[ ]";
                int color = isSelected ? 0x55FF55 : 0x888888;
                graphics.drawString(font, mark, left + width - 24, top + 6, color, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!playerList.active) return false;
            UUID uuid = this.info.getProfile().getId();
            if (selectedMode == AnchorMode.FIXED) {
                selectedPlayers.clear();
                selectedPlayers.add(uuid);
            } else if (selectedMode == AnchorMode.MULTI) {
                if (selectedPlayers.contains(uuid)) {
                    selectedPlayers.remove(uuid);
                } else {
                    selectedPlayers.add(uuid);
                }
            }
            return true;
        }
    }
}