package com.invasion.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.invasion.InvasionMod;
import com.invasion.compat.ConfiguredModMobs;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;

/** Widget-based editor exposed through NeoForge's Mods configuration button. */
public final class InvasionConfigScreen extends Screen {
    private static final Path CFG = FMLPaths.CONFIGDIR.get().resolve("invasion_config.cfg");
    private static final Path MOBS = FMLPaths.CONFIGDIR.get().resolve("invasion_mod_mobs.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PAGE_SIZE = 9;
    private static final int MOB_SEARCH_TOP = 53;
    private static final int MOB_ROWS_TOP = 77;
    private static final int MOB_ROW_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 50;

    private final Screen parent;
    private final Properties properties = new Properties();
    private final List<Label> labels = new ArrayList<>();
    private JsonObject mobs = new JsonObject();
    private boolean mobTab;
    private int page;
    private int mobPageSize = 5;
    private String mobSearch = "";
    private EditBox mobSearchBox;
    private Component status = Component.empty();

    public InvasionConfigScreen(Screen parent) {
        super(Component.translatable("invmod.config.title"));
        this.parent = parent;
        loadFiles();
    }

    private void loadFiles() {
        try {
            properties.clear();
            if (Files.exists(CFG)) {
                try (Reader reader = Files.newBufferedReader(CFG)) {
                    properties.load(reader);
                }
            }
            mobs = Files.exists(MOBS)
                    ? JsonParser.parseString(Files.readString(MOBS)).getAsJsonObject()
                    : new JsonObject();
        } catch (Exception exception) {
            status = Component.literal(message(exception));
        }
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        labels.clear();
        int left = Math.max(10, width / 2 - 245);
        int contentWidth = Math.min(490, width - 20);
        Button cfgTab = addRenderableWidget(Button.builder(Component.literal("invasion_config.cfg"), button -> {
            mobTab = false;
            page = 0;
            rebuild();
        }).bounds(left, 28, contentWidth / 2 - 2, 20).build());
        cfgTab.active = mobTab;
        Button mobsTab = addRenderableWidget(Button.builder(Component.literal("invasion_mod_mobs.json"), button -> {
            mobTab = true;
            page = 0;
            rebuild();
        }).bounds(left + contentWidth / 2 + 2, 28, contentWidth / 2 - 2, 20).build());
        mobsTab.active = !mobTab;

        int newMobPageSize = Math.max(1, (height - MOB_ROWS_TOP - FOOTER_HEIGHT) / MOB_ROW_HEIGHT);
        if (mobTab && newMobPageSize != mobPageSize) {
            // Keep the first previously visible entry on screen after resizing.
            page = page * mobPageSize / newMobPageSize;
        }
        mobPageSize = newMobPageSize;
        int pageSize = mobTab ? mobPageSize : PAGE_SIZE;
        int pages = Math.max(1, (keys().size() + pageSize - 1) / pageSize);
        page = Math.clamp(page, 0, pages - 1);
        if (mobTab) {
            addMobSearch(left, contentWidth);
            addMobRows(left, contentWidth);
        } else {
            addCfgRows(left, contentWidth);
        }
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page--;
            rebuild();
        }).bounds(left, height - 50, 28, 20).build());
        previous.active = page > 0;
        Button pageLabel = addRenderableWidget(Button.builder(
                Component.literal((page + 1) + " / " + pages), button -> {})
                .bounds(left + 32, height - 50, 70, 20).build());
        pageLabel.active = false;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page++;
            rebuild();
        }).bounds(left + 106, height - 50, 28, 20).build());
        next.active = page + 1 < pages;
        addRenderableWidget(Button.builder(Component.translatable("invmod.config.save"), button -> save())
                .bounds(left + contentWidth - 212, height - 50, 100, 20).build());
        Button regenerate = addRenderableWidget(Button.builder(
                Component.translatable("invmod.config.regenerate"), button -> regenerate())
                .bounds(left + contentWidth - 108, height - 50, 108, 20).build());
        regenerate.active = mobTab;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(left + contentWidth - 100, height - 26, 100, 20).build());
    }

    private List<String> keys() {
        if (!mobTab) return properties.stringPropertyNames().stream().sorted().toList();
        String query = mobSearch.strip().toLowerCase(Locale.ROOT);
        return mobs.keySet().stream()
                .filter(key -> !key.startsWith("_"))
                .filter(key -> query.isEmpty() || key.toLowerCase(Locale.ROOT).contains(query))
                .sorted()
                .toList();
    }

    private void addMobSearch(int left, int width) {
        mobSearchBox = new EditBox(font, left, MOB_SEARCH_TOP, width, 20,
                Component.translatable("invmod.config.search"));
        mobSearchBox.setMaxLength(256);
        mobSearchBox.setHint(Component.translatable("invmod.config.search"));
        mobSearchBox.setValue(mobSearch);
        mobSearchBox.setResponder(text -> {
            mobSearch = text;
            page = 0;
            rebuild();
            setFocused(mobSearchBox);
        });
        addRenderableWidget(mobSearchBox);
    }

    private void addCfgRows(int left, int width) {
        List<String> keys = keys();
        for (int index = page * PAGE_SIZE; index < Math.min(keys.size(), (page + 1) * PAGE_SIZE); index++) {
            String key = keys.get(index);
            String value = properties.getProperty(key, "");
            int y = 56 + index % PAGE_SIZE * 24;
            labels.add(new Label(key, left, y + 6));
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                boolean initial = Boolean.parseBoolean(value);
                addRenderableWidget(coloredBooleanBuilder(initial).displayOnlyValue().create(
                        left + width - 130, y, 130, 20, Component.literal(key),
                        (button, selected) -> properties.setProperty(key, selected.toString())));
            } else {
                EditBox field = new EditBox(font, left + width - 180, y, 180, 20, Component.literal(key));
                field.setMaxLength(256);
                field.setValue(value);
                field.setResponder(text -> properties.setProperty(key, text));
                addRenderableWidget(field);
            }
        }
    }

    private void addMobRows(int left, int width) {
        List<String> keys = keys();
        if (keys.isEmpty()) {
            labels.add(new Label(Component.translatable("invmod.config.search.no_results").getString(),
                    left, MOB_ROWS_TOP + 6));
            return;
        }
        for (int index = page * mobPageSize;
                index < Math.min(keys.size(), (page + 1) * mobPageSize); index++) {
            String id = keys.get(index);
            JsonObject mob = mobs.getAsJsonObject(id);
            int y = MOB_ROWS_TOP + index % mobPageSize * MOB_ROW_HEIGHT;
            labels.add(new Label(id, left, y));
            int controlsY = y + 13;
            MobMode mode = mobMode(mob);
            addRenderableWidget(Button.builder(modeLabel(mode), button -> {
                MobMode next = mode.next();
                if (next == MobMode.ATTACK) mob.addProperty("active", "attack");
                else mob.addProperty("active", next == MobMode.ACTIVE);
                rebuild();
            }).bounds(left, controlsY, 82, 20).build());
            if (mode == MobMode.ATTACK) {
                addRenderableWidget(Button.builder(Component.translatable("invmod.config.replace"),
                        button -> minecraft.setScreenAndShow(new MobReplacementScreen(this, id, mob)))
                        .bounds(left + 86, controlsY, 106, 20).build());
                continue;
            }
            boolean boss = mob.has("boss") && mob.get("boss").getAsBoolean();
            addRenderableWidget(coloredBooleanBuilder(boss).create(
                    left + 86, controlsY, 82, 20, Component.translatable("invmod.config.boss"),
                    (button, selected) -> mob.addProperty("boss", selected)));
            labels.add(new Label(Component.translatable("invmod.config.cost").getString(),
                    left + 174, controlsY + 6));
            EditBox cost = new EditBox(font, left + 216, controlsY, 42, 20,
                    Component.translatable("invmod.config.cost"));
            cost.setMaxLength(8);
            cost.setValue(mob.has("cost") ? mob.get("cost").getAsString() : "5");
            cost.setResponder(text -> {
                try {
                    mob.addProperty("cost", Math.max(1, Integer.parseInt(text)));
                } catch (NumberFormatException ignored) {
                }
            });
            addRenderableWidget(cost);
            addRenderableWidget(Button.builder(Component.translatable("invmod.config.abilities"),
                    button -> minecraft.setScreenAndShow(new MobAbilitiesScreen(this, id, mob)))
                    .bounds(left + 262, controlsY, 106, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("invmod.config.themes"),
                    button -> minecraft.setScreenAndShow(new MobThemesScreen(this, id, mob)))
                    .bounds(left + 372, controlsY, Math.max(100, width - 372), 20).build());
        }
    }

    private void save() {
        try {
            if (mobTab) {
                try (Writer writer = Files.newBufferedWriter(MOBS)) { GSON.toJson(mobs, writer); }
                ConfiguredModMobs.refresh();
            } else {
                try (Writer writer = Files.newBufferedWriter(CFG)) { properties.store(writer, "Invasion Mod config"); }
                InvasionMod.getConfig().loadConfig(CFG.toFile());
            }
            status = Component.translatable("invmod.config.saved");
            loadFiles();
            rebuild();
        } catch (Exception exception) {
            status = Component.literal(message(exception));
        }
    }

    private void regenerate() {
        minecraft.setScreenAndShow(new ConfirmScreen(confirmed -> {
            minecraft.setScreenAndShow(this);
            if (confirmed) {
                ConfiguredModMobs.regenerate();
                loadFiles();
                page = 0;
                status = Component.translatable("invmod.config.regenerated");
                rebuild();
            }
        }, Component.translatable("invmod.config.regenerate"),
                Component.translatable("invmod.config.regenerate.confirm")));
    }

    private static String message(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private static CycleButton.Builder<Boolean> coloredBooleanBuilder(boolean initial) {
        return CycleButton.booleanBuilder(
                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED), initial);
    }

    private static MobMode mobMode(JsonObject mob) {
        if (!mob.has("active")) return MobMode.INACTIVE;
        if (mob.get("active").isJsonPrimitive()
                && mob.getAsJsonPrimitive("active").isString()) {
            return "attack".equalsIgnoreCase(mob.get("active").getAsString())
                    ? MobMode.ATTACK : MobMode.INACTIVE;
        }
        return mob.get("active").getAsBoolean() ? MobMode.ACTIVE : MobMode.INACTIVE;
    }

    private static Component modeLabel(MobMode mode) {
        ChatFormatting color = switch (mode) {
            case INACTIVE -> ChatFormatting.RED;
            case ACTIVE -> ChatFormatting.GREEN;
            case ATTACK -> ChatFormatting.GOLD;
        };
        return Component.translatable("invmod.config.active").append(": ")
                .append(Component.translatable("invmod.config.active." + mode.name().toLowerCase(Locale.ROOT))
                        .withStyle(color));
    }

    private enum MobMode {
        INACTIVE, ACTIVE, ATTACK;

        private MobMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        labels.forEach(label -> graphics.text(font, label.text(), label.x(), label.y(), 0xFFFFFFFF, false));
        graphics.text(font, status, Math.max(10, width / 2 - 245), height - 24, 0xFF55FF55, false);
    }

    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
    private record Label(String text, int x, int y) {}

    private static final class MobThemesScreen extends Screen {
        private static final List<String> THEMES = List.of("SWARM", "ARMORED", "RANGED", "UNDERGROUND",
                "SPIDER", "FLYING", "NETHER", "SIEGE", "FAST", "MIXED", "RANDOM", "RANDOMHELL");
        private final InvasionConfigScreen parent;
        private final JsonObject mob;
        private final List<String> selected = new ArrayList<>();

        MobThemesScreen(InvasionConfigScreen parent, String id, JsonObject mob) {
            super(Component.literal(id));
            this.parent = parent;
            this.mob = mob;
            if (mob.has("themes")) mob.getAsJsonArray("themes").forEach(value -> selected.add(value.getAsString()));
        }

        @Override protected void init() {
            for (int index = 0; index < THEMES.size(); index++) {
                String theme = THEMES.get(index);
                boolean enabled = selected.contains(theme);
                int x = width / 2 - 204 + index % 2 * 208;
                int y = 42 + index / 2 * 24;
                addRenderableWidget(coloredBooleanBuilder(enabled).create(x, y, 200, 20,
                        Component.literal(theme), (button, value) -> {
                            if (value && !selected.contains(theme)) selected.add(theme);
                            if (!value) selected.remove(theme);
                        }));
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> done())
                    .bounds(width / 2 - 50, 194, 100, 20).build());
        }

        private void done() {
            JsonArray themes = new JsonArray();
            THEMES.stream().filter(selected::contains).forEach(themes::add);
            mob.add("themes", themes);
            minecraft.setScreenAndShow(parent);
        }

        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            super.extractRenderState(graphics, x, y, delta);
            graphics.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);
        }
        @Override public void onClose() { done(); }
    }

    private static final class MobReplacementScreen extends Screen {
        private static final int PAGE_SIZE = 10;
        private final InvasionConfigScreen parent;
        private final JsonObject mob;
        private String search = "";
        private int page;

        MobReplacementScreen(InvasionConfigScreen parent, String id, JsonObject mob) {
            super(Component.literal(id));
            this.parent = parent;
            this.mob = mob;
        }

        @Override protected void init() {
            clearWidgets();
            int left = Math.max(10, width / 2 - 200);
            int contentWidth = Math.min(400, width - 20);
            EditBox searchBox = new EditBox(font, left, 32, contentWidth, 20,
                    Component.translatable("invmod.config.search"));
            searchBox.setHint(Component.translatable("invmod.config.search"));
            searchBox.setMaxLength(256);
            searchBox.setValue(search);
            searchBox.setResponder(value -> {
                search = value;
                page = 0;
                init();
                setFocused(children().getFirst());
            });
            addRenderableWidget(searchBox);

            List<String> ids = entityIds();
            int pages = Math.max(1, (ids.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            page = Math.clamp(page, 0, pages - 1);
            for (int index = page * PAGE_SIZE;
                    index < Math.min(ids.size(), (page + 1) * PAGE_SIZE); index++) {
                String id = ids.get(index);
                int y = 58 + index % PAGE_SIZE * 22;
                addRenderableWidget(Button.builder(Component.literal(id), button -> select(id))
                        .bounds(left, y, contentWidth, 20).build());
            }
            Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                page--;
                init();
            }).bounds(left, height - 28, 28, 20).build());
            previous.active = page > 0;
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                page++;
                init();
            }).bounds(left + contentWidth - 28, height - 28, 28, 20).build());
            next.active = page + 1 < pages;
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                    .bounds(width / 2 - 50, height - 28, 100, 20).build());
        }

        private List<String> entityIds() {
            String query = search.strip().toLowerCase(Locale.ROOT);
            return BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .filter(key -> BuiltInRegistries.ENTITY_TYPE.getValue(key).canSummon())
                    .map(Identifier::toString)
                    .filter(id -> query.isEmpty() || id.toLowerCase(Locale.ROOT).contains(query))
                    .sorted().toList();
        }

        private void select(String id) {
            mob.addProperty("replace", id);
            minecraft.setScreenAndShow(parent);
        }

        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            super.extractRenderState(graphics, x, y, delta);
            graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        }
        @Override public void onClose() { minecraft.setScreenAndShow(parent); }
    }

    private static final class MobAbilitiesScreen extends Screen {
        private static final List<String> ABILITIES = List.of(
                "weapons", "armor", "mining", "stairing", "bridging", "towering");
        private final InvasionConfigScreen parent;
        private final JsonObject mob;
        private final JsonObject abilities;

        MobAbilitiesScreen(InvasionConfigScreen parent, String id, JsonObject mob) {
            super(Component.literal(id));
            this.parent = parent;
            this.mob = mob;
            this.abilities = mob.has("abilities") && mob.get("abilities").isJsonObject()
                    ? mob.getAsJsonObject("abilities").deepCopy() : new JsonObject();
            migrateLegacy("weapons", "canUseWeapons");
            migrateLegacy("armor", "canWearArmor");
        }

        private void migrateLegacy(String ability, String legacy) {
            if (!abilities.has(ability)) {
                abilities.addProperty(ability,
                        mob.has(legacy) && mob.get(legacy).getAsBoolean());
            }
        }

        @Override protected void init() {
            for (int index = 0; index < ABILITIES.size(); index++) {
                String ability = ABILITIES.get(index);
                boolean enabled = abilities.has(ability)
                        && abilities.get(ability).getAsBoolean();
                int x = width / 2 - 204 + index % 2 * 208;
                int y = 42 + index / 2 * 24;
                addRenderableWidget(coloredBooleanBuilder(enabled).create(x, y, 200, 20,
                        Component.translatable("invmod.config.ability." + ability),
                        (button, value) -> abilities.addProperty(ability, value)));
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> done())
                    .bounds(width / 2 - 50, 122, 100, 20).build());
        }

        private void done() {
            ABILITIES.forEach(ability -> {
                if (!abilities.has(ability)) abilities.addProperty(ability, false);
            });
            mob.add("abilities", abilities);
            mob.remove("canUseWeapons");
            mob.remove("canWearArmor");
            minecraft.setScreenAndShow(parent);
        }

        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            super.extractRenderState(graphics, x, y, delta);
            graphics.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);
        }
        @Override public void onClose() { done(); }
    }
}
