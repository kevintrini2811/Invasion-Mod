package com.invasion.client.screen;

import com.google.gson.JsonParser;
import com.invasion.InvasionMod;
import com.invasion.compat.ConfiguredModMobs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

/** Raw in-game editors for Invasion's legacy and dynamic configuration files. */
public final class InvasionConfigScreen extends Screen {
    private static final Path LEGACY_FILE = FMLPaths.CONFIGDIR.get().resolve("invasion_config.cfg");
    private static final Path MOBS_FILE = FMLPaths.CONFIGDIR.get().resolve("invasion_mod_mobs.json");

    private final Screen parent;
    private MultiLineEditBox editor;
    private Button legacyTab;
    private Button mobsTab;
    private Button regenerate;
    private Path selected = LEGACY_FILE;
    private Component status = Component.empty();
    private int statusColor = 0xFFA0A0A0;

    public InvasionConfigScreen(Screen parent) {
        super(Component.translatable("invmod.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int left = Math.max(10, width / 2 - 240);
        int contentWidth = Math.min(480, width - 20);
        legacyTab = addRenderableWidget(Button.builder(
                Component.literal("invasion_config.cfg"), button -> select(LEGACY_FILE))
                .bounds(left, 28, contentWidth / 2 - 2, 20).build());
        mobsTab = addRenderableWidget(Button.builder(
                Component.literal("invasion_mod_mobs.json"), button -> select(MOBS_FILE))
                .bounds(left + contentWidth / 2 + 2, 28, contentWidth / 2 - 2, 20).build());
        editor = addRenderableWidget(new MultiLineEditBox.Builder().setX(left).setY(54)
                .setPlaceholder(Component.translatable("invmod.config.empty"))
                .build(font, contentWidth, Math.max(80, height - 112),
                        Component.translatable("invmod.config.editor")));
        editor.setCharacterLimit(1_000_000);
        addRenderableWidget(Button.builder(Component.translatable("invmod.config.save"), button -> save())
                .bounds(left, height - 52, 100, 20).build());
        regenerate = addRenderableWidget(Button.builder(Component.translatable("invmod.config.regenerate"),
                button -> confirmRegenerate()).bounds(left + 104, height - 52, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(left + contentWidth - 100, height - 52, 100, 20).build());
        loadSelected();
        updateButtons();
    }

    private void select(Path file) {
        selected = file;
        status = Component.empty();
        loadSelected();
        updateButtons();
    }

    private void loadSelected() {
        try {
            editor.setValue(Files.exists(selected) ? Files.readString(selected) : "", true);
        } catch (IOException exception) {
            fail(exception);
        }
    }

    private void updateButtons() {
        legacyTab.active = !selected.equals(LEGACY_FILE);
        mobsTab.active = !selected.equals(MOBS_FILE);
        regenerate.active = selected.equals(MOBS_FILE);
    }

    private void save() {
        try {
            if (selected.equals(MOBS_FILE)) {
                JsonParser.parseString(editor.getValue()).getAsJsonObject();
            }
            writeAtomically(selected, editor.getValue());
            if (selected.equals(MOBS_FILE)) ConfiguredModMobs.refresh();
            else InvasionMod.getConfig().loadConfig(LEGACY_FILE.toFile());
            status = Component.translatable("invmod.config.saved");
            statusColor = 0xFF55FF55;
            loadSelected();
        } catch (Exception exception) {
            fail(exception);
        }
    }

    private void confirmRegenerate() {
        minecraft.setScreenAndShow(new ConfirmScreen(confirmed -> {
            minecraft.setScreenAndShow(this);
            if (confirmed) {
                ConfiguredModMobs.regenerate();
                selected = MOBS_FILE;
                loadSelected();
                updateButtons();
                status = Component.translatable("invmod.config.regenerated");
                statusColor = 0xFFFFFF55;
            }
        }, Component.translatable("invmod.config.regenerate"),
                Component.translatable("invmod.config.regenerate.confirm")));
    }

    private static void writeAtomically(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, content);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void fail(Exception exception) {
        InvasionMod.LOGGER.error("Could not edit Invasion config {}", selected, exception);
        status = Component.literal(exception.getMessage() == null
                ? exception.getClass().getSimpleName() : exception.getMessage());
        statusColor = 0xFFFF5555;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        graphics.text(font, status, Math.max(10, width / 2 - 240), height - 25,
                statusColor, false);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }
}
