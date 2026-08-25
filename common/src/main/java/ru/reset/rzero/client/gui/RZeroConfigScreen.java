package ru.reset.rzero.client.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntSliderBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.reset.rzero.RZeroConfig;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.config.RZeroClientRestoreSettings;
import ru.reset.rzero.config.RZeroAdaptiveSettings;
import ru.reset.rzero.config.RZeroSettings;
import ru.reset.rzero.runtime.RZeroRuntime;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class RZeroConfigScreen {

    public enum UiMode {
        BASIC,
        EXPERT
    }

    private static UiMode currentUiMode = UiMode.BASIC;

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.rzero.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        RZeroSettings current = RZeroRuntime.settings();

        class StateHolder {
            UiMode uiMode = currentUiMode;
            boolean rzerochashEnabled = current.rzerochashEnabled();

            int adaptive_minSaveGapSeconds = current.adaptive().minSaveGapSeconds();
            int adaptive_relaxTimeSeconds = current.adaptive().relaxTimeSeconds();
            int adaptive_postCombatSavePercent = current.adaptive().postCombatSavePercent();
            boolean adaptive_saveOnBossDefeat = current.adaptive().saveOnBossDefeat();

            boolean rollback_blocks = current.checkpointPolicy().rollback().blocks();
            boolean rollback_blockEntities = current.checkpointPolicy().rollback().blockEntities();
            boolean rollback_blockTicks = current.checkpointPolicy().rollback().blockTicks();
            boolean rollback_fluidTicks = current.checkpointPolicy().rollback().fluidTicks();
            boolean rollback_blockEvents = current.checkpointPolicy().rollback().blockEvents();
            boolean rollback_pois = current.checkpointPolicy().rollback().pois();

            boolean rollback_players_position = current.checkpointPolicy().rollback().players().position();
            boolean rollback_players_motion = current.checkpointPolicy().rollback().players().motion();
            boolean rollback_players_health = current.checkpointPolicy().rollback().players().health();
            boolean rollback_players_hunger = current.checkpointPolicy().rollback().players().hunger();
            boolean rollback_players_experience = current.checkpointPolicy().rollback().players().experience();
            boolean rollback_players_inventory = current.checkpointPolicy().rollback().players().inventory();
            boolean rollback_players_enderChest = current.checkpointPolicy().rollback().players().enderChest();
            boolean rollback_players_potionEffects = current.checkpointPolicy().rollback().players().potionEffects();
            boolean rollback_players_cooldowns = current.checkpointPolicy().rollback().players().cooldowns();
            boolean rollback_players_openMenu = current.checkpointPolicy().rollback().players().openMenu();
            boolean rollback_players_abilities = current.checkpointPolicy().rollback().players().abilities();
            boolean rollback_players_recipeBook = current.checkpointPolicy().rollback().players().recipeBook();
            boolean rollback_players_spawnPoint = current.checkpointPolicy().rollback().players().spawnPoint();
            boolean rollback_players_score = current.checkpointPolicy().rollback().players().score();
            boolean rollback_players_preserveNewPlayerInventory = current.checkpointPolicy().rollback().players().preserveNewPlayerInventory();

            boolean rollback_entities_presence = current.checkpointPolicy().rollback().entities().presence();
            boolean rollback_entities_entityId = current.checkpointPolicy().rollback().entities().entityId();
            boolean rollback_entities_tickCount = current.checkpointPolicy().rollback().entities().tickCount();
            boolean rollback_entities_rngState = current.checkpointPolicy().rollback().entities().rngState();
            boolean rollback_entities_livingTimers = current.checkpointPolicy().rollback().entities().livingTimers();
            boolean rollback_entities_target = current.checkpointPolicy().rollback().entities().target();
            boolean rollback_entities_navigation = current.checkpointPolicy().rollback().entities().navigation();
            boolean rollback_entities_brainRam = current.checkpointPolicy().rollback().entities().brainRam();
            boolean rollback_entities_passengers = current.checkpointPolicy().rollback().entities().passengers();
            boolean rollback_entities_mobRamCache = current.checkpointPolicy().rollback().entities().mobRamCache();
            boolean rollback_entities_droppedItems = current.checkpointPolicy().rollback().entities().droppedItems();
            boolean rollback_entities_experienceOrbs = current.checkpointPolicy().rollback().entities().experienceOrbs();

            boolean rollback_world_time = current.checkpointPolicy().rollback().world().time();
            boolean rollback_world_weather = current.checkpointPolicy().rollback().world().weather();
            boolean rollback_world_raids = current.checkpointPolicy().rollback().world().raids();
            boolean rollback_world_dragonFight = current.checkpointPolicy().rollback().world().dragonFight();
            boolean rollback_world_scoreboard = current.checkpointPolicy().rollback().world().scoreboard();
            boolean rollback_world_levelRng = current.checkpointPolicy().rollback().world().levelRng();

            boolean rollback_serverGlobals_bossbars = current.checkpointPolicy().rollback().world().serverGlobals().bossbars();
            boolean rollback_serverGlobals_forcedChunks = current.checkpointPolicy().rollback().world().serverGlobals().forcedChunks();
            boolean rollback_serverGlobals_worldBorder = current.checkpointPolicy().rollback().world().serverGlobals().worldBorder();
            boolean rollback_serverGlobals_gameRules = current.checkpointPolicy().rollback().world().serverGlobals().gameRules();
            boolean rollback_serverGlobals_randomSequences = current.checkpointPolicy().rollback().world().serverGlobals().randomSequences();
            boolean rollback_serverGlobals_savedData = current.checkpointPolicy().rollback().world().serverGlobals().savedData();
            boolean rollback_serverGlobals_serverTickCount = current.checkpointPolicy().rollback().world().serverGlobals().serverTickCount();
            boolean rollback_serverGlobals_shufflingCounter = current.checkpointPolicy().rollback().world().serverGlobals().shufflingCounter();

            boolean det_spawn_enabled = current.checkpointPolicy().determinism().naturalSpawn().enabled();
            boolean det_spawn_engine = current.checkpointPolicy().determinism().naturalSpawn().useSpawnEngine();
            boolean det_spawn_localCap = current.checkpointPolicy().determinism().naturalSpawn().localCap();
            boolean det_spawn_catchUp = current.checkpointPolicy().determinism().naturalSpawn().catchUp();
            boolean det_spawn_monsterCatchUp = current.checkpointPolicy().determinism().naturalSpawn().monsterCatchUp();
            boolean det_spawn_stamp = current.checkpointPolicy().determinism().naturalSpawn().stampSpawnOrigin();

            boolean det_ai_brainRng = current.checkpointPolicy().determinism().mobAi().brainRng();
            boolean det_ai_behaviorDuration = current.checkpointPolicy().determinism().mobAi().behaviorDurationRng();
            boolean det_ai_shufflingList = current.checkpointPolicy().determinism().mobAi().shufflingListRng();
            boolean det_ai_memoryOrder = current.checkpointPolicy().determinism().mobAi().memoryIterationOrder();

            boolean det_loot_container = current.checkpointPolicy().determinism().loot().containerLoot();
            boolean det_loot_mobDrops = current.checkpointPolicy().determinism().loot().mobDeathDrops();

            boolean det_spawns_raidSeed = current.checkpointPolicy().determinism().spawns().raidSeed();
            boolean det_spawns_raidPos = current.checkpointPolicy().determinism().spawns().raidSpawnPosition();
            boolean det_spawns_trader = current.checkpointPolicy().determinism().spawns().wanderingTrader();

            boolean det_combat_knockback = current.checkpointPolicy().determinism().combat().knockbackRng();

            boolean client_cameraType = current.clientRestore().cameraType();
            boolean client_stopFutureSounds = current.clientRestore().stopFutureSounds();
            boolean client_clearParticles = current.clientRestore().clearParticles();
            boolean client_restoreInventoryScreen = current.clientRestore().restoreInventoryScreen();
            boolean client_restoreCreativeTab = current.clientRestore().restoreCreativeTab();
            boolean client_restoreRecipeBookTab = current.clientRestore().restoreRecipeBookTab();
            boolean client_restoreSearchText = current.clientRestore().restoreSearchText();
            boolean client_resetHurtAnimation = current.clientRestore().resetHurtAnimation();
            boolean client_snapPlayerRotation = current.clientRestore().snapPlayerRotation();
            boolean client_skipEquipAnimation = current.clientRestore().skipEquipAnimation();
            boolean client_restoreChatState = current.clientRestore().restoreChatState();
            boolean client_suppressTerrainScreen = current.clientRestore().suppressTerrainLoadingScreen();
            boolean client_displayChatNotifications = current.clientRestore().displayChatNotifications();
            boolean client_meshCacheEnabled = current.clientRestore().meshCacheEnabled();
            int client_meshCacheRadius = current.clientRestore().meshCacheRadius();
            int client_meshCacheBudgetMb = current.clientRestore().meshCacheBudgetMb();
        }

        StateHolder state = new StateHolder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.rzero.category.general"));

        SelectionListEntry<UiMode> uiModeEntry = new SelectionListEntry<>(
                Component.translatable("config.rzero.uiMode"),
                UiMode.values(),
                currentUiMode,
                Component.translatable("text.cloth-config.reset_value"),
                () -> UiMode.BASIC,
                val -> {
                    state.uiMode = val;
                    currentUiMode = val;
                },
                mode -> Component.translatable("config.rzero.uiMode." + mode.name().toLowerCase(Locale.ROOT)),
                () -> Optional.of(new Component[]{Component.translatable("config.rzero.uiMode.tooltip")}),
                false
        ) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                boolean handled = super.mouseClicked(mouseX, mouseY, button);
                if (handled) {
                    UiMode next = getValue();
                    if (next != currentUiMode) {
                        currentUiMode = next;
                        Minecraft.getInstance().setScreen(RZeroConfigScreen.create(parent));
                    }
                }
                return handled;
            }
        };
        general.addEntry(uiModeEntry);

        ConfigCategory clientRestore = builder.getOrCreateCategory(Component.translatable("config.rzero.category.clientRestore"));

        addToggle(clientRestore, entryBuilder, "config.rzero.rzerochashEnabled", state.rzerochashEnabled, true, val -> state.rzerochashEnabled = val, true);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_displayChatNotifications", state.client_displayChatNotifications, true, val -> state.client_displayChatNotifications = val, false);

        if (currentUiMode == UiMode.EXPERT) {
            addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_meshCacheEnabled", state.client_meshCacheEnabled, false, val -> state.client_meshCacheEnabled = val, true);
            addSlider(clientRestore, entryBuilder, "config.rzero.clientRestore_meshCacheRadius", state.client_meshCacheRadius, RZeroClientRestoreSettings.MIN_MESH_RADIUS, RZeroClientRestoreSettings.MAX_MESH_RADIUS, 12, val -> state.client_meshCacheRadius = val, true);
            addSlider(clientRestore, entryBuilder, "config.rzero.clientRestore_meshCacheBudgetMb", state.client_meshCacheBudgetMb, 64, RZeroClientRestoreSettings.MAX_MESH_BUDGET_MB, 512, val -> state.client_meshCacheBudgetMb = val, true);
        }

        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_cameraType", state.client_cameraType, true, val -> state.client_cameraType = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_stopFutureSounds", state.client_stopFutureSounds, true, val -> state.client_stopFutureSounds = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_clearParticles", state.client_clearParticles, true, val -> state.client_clearParticles = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_resetHurtAnimation", state.client_resetHurtAnimation, true, val -> state.client_resetHurtAnimation = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_restoreInventoryScreen", state.client_restoreInventoryScreen, true, val -> state.client_restoreInventoryScreen = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_restoreCreativeTab", state.client_restoreCreativeTab, true, val -> state.client_restoreCreativeTab = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_restoreRecipeBookTab", state.client_restoreRecipeBookTab, true, val -> state.client_restoreRecipeBookTab = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_restoreSearchText", state.client_restoreSearchText, true, val -> state.client_restoreSearchText = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_snapPlayerRotation", state.client_snapPlayerRotation, true, val -> state.client_snapPlayerRotation = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_skipEquipAnimation", state.client_skipEquipAnimation, true, val -> state.client_skipEquipAnimation = val, false);
        addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_restoreChatState", state.client_restoreChatState, true, val -> state.client_restoreChatState = val, false);

        if (currentUiMode == UiMode.EXPERT) {
            addToggle(clientRestore, entryBuilder, "config.rzero.clientRestore_suppressTerrainLoadingScreen", state.client_suppressTerrainScreen, false, val -> state.client_suppressTerrainScreen = val, false);
        }

        ConfigCategory adaptive = builder.getOrCreateCategory(Component.translatable("config.rzero.category.adaptive"));
        addToggle(adaptive, entryBuilder, "config.rzero.adaptive_saveOnBossDefeat", state.adaptive_saveOnBossDefeat, true, val -> state.adaptive_saveOnBossDefeat = val, false);
        addSlider(adaptive, entryBuilder, "config.rzero.adaptive_postCombatSavePercent", state.adaptive_postCombatSavePercent, 0, 100, 70, val -> state.adaptive_postCombatSavePercent = val, false);

        if (currentUiMode == UiMode.EXPERT) {
            addSlider(adaptive, entryBuilder, "config.rzero.adaptive_minSaveGapSeconds", state.adaptive_minSaveGapSeconds, 5, 300, 30, val -> state.adaptive_minSaveGapSeconds = val, false);
            addSlider(adaptive, entryBuilder, "config.rzero.adaptive_relaxTimeSeconds", state.adaptive_relaxTimeSeconds, 1, 30, 5, val -> state.adaptive_relaxTimeSeconds = val, false);
        }

        ConfigCategory policy = builder.getOrCreateCategory(Component.translatable("config.rzero.category.policy"));

        SubCategoryBuilder subRollbackPlayers = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.rollback_players")).setExpanded(false);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_experience", state.rollback_players_experience, true, val -> state.rollback_players_experience = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_health", state.rollback_players_health, true, val -> state.rollback_players_health = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_hunger", state.rollback_players_hunger, true, val -> state.rollback_players_hunger = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_inventory", state.rollback_players_inventory, true, val -> state.rollback_players_inventory = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_enderChest", state.rollback_players_enderChest, true, val -> state.rollback_players_enderChest = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_position", state.rollback_players_position, true, val -> state.rollback_players_position = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_motion", state.rollback_players_motion, true, val -> state.rollback_players_motion = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_potionEffects", state.rollback_players_potionEffects, true, val -> state.rollback_players_potionEffects = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_abilities", state.rollback_players_abilities, true, val -> state.rollback_players_abilities = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_spawnPoint", state.rollback_players_spawnPoint, true, val -> state.rollback_players_spawnPoint = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_score", state.rollback_players_score, true, val -> state.rollback_players_score = val);
        addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_preserveNewPlayerInventory", state.rollback_players_preserveNewPlayerInventory, false, val -> state.rollback_players_preserveNewPlayerInventory = val);

        if (currentUiMode == UiMode.EXPERT) {
            addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_cooldowns", state.rollback_players_cooldowns, true, val -> state.rollback_players_cooldowns = val);
            addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_openMenu", state.rollback_players_openMenu, true, val -> state.rollback_players_openMenu = val);
            addSubToggle(subRollbackPlayers, entryBuilder, "config.rzero.checkpointPolicy_rollback_players_recipeBook", state.rollback_players_recipeBook, true, val -> state.rollback_players_recipeBook = val);
        }
        policy.addEntry(subRollbackPlayers.build());

        SubCategoryBuilder subRollbackWorld = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.rollback_world")).setExpanded(false);
        addSubToggle(subRollbackWorld, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_weather", state.rollback_world_weather, true, val -> state.rollback_world_weather = val);
        addSubToggle(subRollbackWorld, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_time", state.rollback_world_time, true, val -> state.rollback_world_time = val);
        addSubToggle(subRollbackWorld, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_raids", state.rollback_world_raids, true, val -> state.rollback_world_raids = val);
        addSubToggle(subRollbackWorld, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_dragonFight", state.rollback_world_dragonFight, true, val -> state.rollback_world_dragonFight = val);

        if (currentUiMode == UiMode.EXPERT) {
            addSubToggle(subRollbackWorld, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_scoreboard", state.rollback_world_scoreboard, true, val -> state.rollback_world_scoreboard = val);
            addSubToggle(subRollbackWorld, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_levelRng", state.rollback_world_levelRng, true, val -> state.rollback_world_levelRng = val);
        }
        policy.addEntry(subRollbackWorld.build());

        SubCategoryBuilder subRollbackBlocks = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.rollback")).setExpanded(false);
        addSubToggle(subRollbackBlocks, entryBuilder, "config.rzero.checkpointPolicy_rollback_blocks", state.rollback_blocks, true, val -> state.rollback_blocks = val);
        addSubToggle(subRollbackBlocks, entryBuilder, "config.rzero.checkpointPolicy_rollback_blockEntities", state.rollback_blockEntities, true, val -> state.rollback_blockEntities = val);

        if (currentUiMode == UiMode.EXPERT) {
            addSubToggle(subRollbackBlocks, entryBuilder, "config.rzero.checkpointPolicy_rollback_blockTicks", state.rollback_blockTicks, true, val -> state.rollback_blockTicks = val);
            addSubToggle(subRollbackBlocks, entryBuilder, "config.rzero.checkpointPolicy_rollback_fluidTicks", state.rollback_fluidTicks, true, val -> state.rollback_fluidTicks = val);
            addSubToggle(subRollbackBlocks, entryBuilder, "config.rzero.checkpointPolicy_rollback_blockEvents", state.rollback_blockEvents, true, val -> state.rollback_blockEvents = val);
            addSubToggle(subRollbackBlocks, entryBuilder, "config.rzero.checkpointPolicy_rollback_pois", state.rollback_pois, true, val -> state.rollback_pois = val);
        }
        policy.addEntry(subRollbackBlocks.build());

        SubCategoryBuilder subRollbackEntities = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.rollback_entities")).setExpanded(false);
        addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_presence", state.rollback_entities_presence, true, val -> state.rollback_entities_presence = val);
        addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_droppedItems", state.rollback_entities_droppedItems, true, val -> state.rollback_entities_droppedItems = val);
        addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_experienceOrbs", state.rollback_entities_experienceOrbs, true, val -> state.rollback_entities_experienceOrbs = val);

        if (currentUiMode == UiMode.EXPERT) {
            addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_entityId", state.rollback_entities_entityId, true, val -> state.rollback_entities_entityId = val);
            addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_tickCount", state.rollback_entities_tickCount, true, val -> state.rollback_entities_tickCount = val);
            addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_rngState", state.rollback_entities_rngState, true, val -> state.rollback_entities_rngState = val);
            addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_livingTimers", state.rollback_entities_livingTimers, true, val -> state.rollback_entities_livingTimers = val);
            addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_target", state.rollback_entities_target, true, val -> state.rollback_entities_target = val);
            addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_navigation", state.rollback_entities_navigation, true, val -> state.rollback_entities_navigation = val);
            addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_brainRam", state.rollback_entities_brainRam, true, val -> state.rollback_entities_brainRam = val);
            addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_passengers", state.rollback_entities_passengers, true, val -> state.rollback_entities_passengers = val);
            addSubToggle(subRollbackEntities, entryBuilder, "config.rzero.checkpointPolicy_rollback_entities_mobRamCache", state.rollback_entities_mobRamCache, true, val -> state.rollback_entities_mobRamCache = val);
        }
        policy.addEntry(subRollbackEntities.build());

        if (currentUiMode == UiMode.EXPERT) {
            SubCategoryBuilder subServerGlobals = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.rollback_world_serverglobals")).setExpanded(false);
            addSubToggle(subServerGlobals, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_serverGlobals_bossbars", state.rollback_serverGlobals_bossbars, true, val -> state.rollback_serverGlobals_bossbars = val);
            addSubToggle(subServerGlobals, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_serverGlobals_forcedChunks", state.rollback_serverGlobals_forcedChunks, true, val -> state.rollback_serverGlobals_forcedChunks = val);
            addSubToggle(subServerGlobals, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_serverGlobals_worldBorder", state.rollback_serverGlobals_worldBorder, true, val -> state.rollback_serverGlobals_worldBorder = val);
            addSubToggle(subServerGlobals, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_serverGlobals_gameRules", state.rollback_serverGlobals_gameRules, true, val -> state.rollback_serverGlobals_gameRules = val);
            addSubToggle(subServerGlobals, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_serverGlobals_randomSequences", state.rollback_serverGlobals_randomSequences, true, val -> state.rollback_serverGlobals_randomSequences = val);
            addSubToggle(subServerGlobals, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_serverGlobals_savedData", state.rollback_serverGlobals_savedData, true, val -> state.rollback_serverGlobals_savedData = val);
            addSubToggle(subServerGlobals, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_serverGlobals_serverTickCount", state.rollback_serverGlobals_serverTickCount, true, val -> state.rollback_serverGlobals_serverTickCount = val);
            addSubToggle(subServerGlobals, entryBuilder, "config.rzero.checkpointPolicy_rollback_world_serverGlobals_shufflingCounter", state.rollback_serverGlobals_shufflingCounter, true, val -> state.rollback_serverGlobals_shufflingCounter = val);
            policy.addEntry(subServerGlobals.build());

            ConfigCategory determinism = builder.getOrCreateCategory(Component.translatable("config.rzero.category.determinism"));

            SubCategoryBuilder subNaturalSpawn = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.determinism_naturalspawn")).setExpanded(false);
            addSubToggle(subNaturalSpawn, entryBuilder, "config.rzero.checkpointPolicy_determinism_naturalSpawn_enabled", state.det_spawn_enabled, true, val -> state.det_spawn_enabled = val);
            addSubToggle(subNaturalSpawn, entryBuilder, "config.rzero.checkpointPolicy_determinism_naturalSpawn_useSpawnEngine", state.det_spawn_engine, true, val -> state.det_spawn_engine = val);
            addSubToggle(subNaturalSpawn, entryBuilder, "config.rzero.checkpointPolicy_determinism_naturalSpawn_localCap", state.det_spawn_localCap, false, val -> state.det_spawn_localCap = val, true);
            addSubToggle(subNaturalSpawn, entryBuilder, "config.rzero.checkpointPolicy_determinism_naturalSpawn_catchUp", state.det_spawn_catchUp, false, val -> state.det_spawn_catchUp = val, true);
            addSubToggle(subNaturalSpawn, entryBuilder, "config.rzero.checkpointPolicy_determinism_naturalSpawn_monsterCatchUp", state.det_spawn_monsterCatchUp, false, val -> state.det_spawn_monsterCatchUp = val, true);
            addSubToggle(subNaturalSpawn, entryBuilder, "config.rzero.checkpointPolicy_determinism_naturalSpawn_stampSpawnOrigin", state.det_spawn_stamp, true, val -> state.det_spawn_stamp = val);
            determinism.addEntry(subNaturalSpawn.build());

            SubCategoryBuilder subMobAi = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.determinism_mobai")).setExpanded(false);
            addSubToggle(subMobAi, entryBuilder, "config.rzero.checkpointPolicy_determinism_mobAi_brainRng", state.det_ai_brainRng, true, val -> state.det_ai_brainRng = val);
            addSubToggle(subMobAi, entryBuilder, "config.rzero.checkpointPolicy_determinism_mobAi_behaviorDurationRng", state.det_ai_behaviorDuration, true, val -> state.det_ai_behaviorDuration = val);
            addSubToggle(subMobAi, entryBuilder, "config.rzero.checkpointPolicy_determinism_mobAi_shufflingListRng", state.det_ai_shufflingList, true, val -> state.det_ai_shufflingList = val);
            addSubToggle(subMobAi, entryBuilder, "config.rzero.checkpointPolicy_determinism_mobAi_memoryIterationOrder", state.det_ai_memoryOrder, true, val -> state.det_ai_memoryOrder = val);
            determinism.addEntry(subMobAi.build());

            SubCategoryBuilder subLoot = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.determinism_loot")).setExpanded(false);
            addSubToggle(subLoot, entryBuilder, "config.rzero.checkpointPolicy_determinism_loot_containerLoot", state.det_loot_container, true, val -> state.det_loot_container = val);
            addSubToggle(subLoot, entryBuilder, "config.rzero.checkpointPolicy_determinism_loot_mobDeathDrops", state.det_loot_mobDrops, true, val -> state.det_loot_mobDrops = val);
            determinism.addEntry(subLoot.build());

            SubCategoryBuilder subSpawns = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.determinism_spawns")).setExpanded(false);
            addSubToggle(subSpawns, entryBuilder, "config.rzero.checkpointPolicy_determinism_spawns_raidSeed", state.det_spawns_raidSeed, true, val -> state.det_spawns_raidSeed = val);
            addSubToggle(subSpawns, entryBuilder, "config.rzero.checkpointPolicy_determinism_spawns_raidSpawnPosition", state.det_spawns_raidPos, true, val -> state.det_spawns_raidPos = val);
            addSubToggle(subSpawns, entryBuilder, "config.rzero.checkpointPolicy_determinism_spawns_wanderingTrader", state.det_spawns_trader, true, val -> state.det_spawns_trader = val);
            determinism.addEntry(subSpawns.build());

            SubCategoryBuilder subCombat = entryBuilder.startSubCategory(Component.translatable("config.rzero.subcategory.determinism_combat")).setExpanded(false);
            addSubToggle(subCombat, entryBuilder, "config.rzero.checkpointPolicy_determinism_combat_knockbackRng", state.det_combat_knockback, true, val -> state.det_combat_knockback = val);
            determinism.addEntry(subCombat.build());
        }

        builder.setSavingRunnable(() -> {
            RZeroCheckpointPolicy.Rollback newRollback = new RZeroCheckpointPolicy.Rollback(
                    state.rollback_blocks,
                    state.rollback_blockEntities,
                    state.rollback_blockTicks,
                    state.rollback_fluidTicks,
                    state.rollback_blockEvents,
                    state.rollback_pois,
                    new RZeroCheckpointPolicy.Players(
                            state.rollback_players_position,
                            state.rollback_players_motion,
                            state.rollback_players_health,
                            state.rollback_players_hunger,
                            state.rollback_players_experience,
                            state.rollback_players_inventory,
                            state.rollback_players_enderChest,
                            state.rollback_players_potionEffects,
                            state.rollback_players_cooldowns,
                            state.rollback_players_openMenu,
                            state.rollback_players_abilities,
                            state.rollback_players_recipeBook,
                            state.rollback_players_spawnPoint,
                            state.rollback_players_score,
                            state.rollback_players_preserveNewPlayerInventory
                    ),
                    new RZeroCheckpointPolicy.Entities(
                            state.rollback_entities_presence,
                            state.rollback_entities_entityId,
                            state.rollback_entities_tickCount,
                            state.rollback_entities_rngState,
                            state.rollback_entities_livingTimers,
                            state.rollback_entities_target,
                            state.rollback_entities_navigation,
                            state.rollback_entities_brainRam,
                            state.rollback_entities_passengers,
                            state.rollback_entities_mobRamCache,
                            state.rollback_entities_droppedItems,
                            state.rollback_entities_experienceOrbs
                    ),
                    new RZeroCheckpointPolicy.World(
                            state.rollback_world_time,
                            state.rollback_world_weather,
                            state.rollback_world_raids,
                            state.rollback_world_dragonFight,
                            state.rollback_world_scoreboard,
                            state.rollback_world_levelRng,
                            new RZeroCheckpointPolicy.ServerGlobals(
                                    state.rollback_serverGlobals_bossbars,
                                    state.rollback_serverGlobals_forcedChunks,
                                    state.rollback_serverGlobals_worldBorder,
                                    state.rollback_serverGlobals_gameRules,
                                    state.rollback_serverGlobals_randomSequences,
                                    state.rollback_serverGlobals_savedData,
                                    state.rollback_serverGlobals_serverTickCount,
                                    state.rollback_serverGlobals_shufflingCounter
                            )
                    )
            );

            RZeroCheckpointPolicy.Determinism newDeterminism = new RZeroCheckpointPolicy.Determinism(
                    new RZeroCheckpointPolicy.NaturalSpawn(
                            state.det_spawn_enabled,
                            state.det_spawn_engine,
                            state.det_spawn_localCap,
                            state.det_spawn_catchUp,
                            state.det_spawn_monsterCatchUp,
                            state.det_spawn_stamp
                    ),
                    new RZeroCheckpointPolicy.MobAi(
                            state.det_ai_brainRng,
                            state.det_ai_behaviorDuration,
                            state.det_ai_shufflingList,
                            state.det_ai_memoryOrder
                    ),
                    new RZeroCheckpointPolicy.Loot(
                            state.det_loot_container,
                            state.det_loot_mobDrops
                    ),
                    new RZeroCheckpointPolicy.Spawns(
                            state.det_spawns_raidSeed,
                            state.det_spawns_raidPos,
                            state.det_spawns_trader
                    ),
                    new RZeroCheckpointPolicy.Combat(
                            state.det_combat_knockback
                    )
            );

            RZeroCheckpointPolicy newPolicy = new RZeroCheckpointPolicy(newRollback, newDeterminism);

            RZeroClientRestoreSettings newClientRestore = new RZeroClientRestoreSettings(
                    state.client_cameraType,
                    state.client_stopFutureSounds,
                    state.client_clearParticles,
                    state.client_restoreInventoryScreen,
                    state.client_restoreCreativeTab,
                    state.client_restoreRecipeBookTab,
                    state.client_restoreSearchText,
                    state.client_resetHurtAnimation,
                    state.client_snapPlayerRotation,
                    state.client_skipEquipAnimation,
                    state.client_restoreChatState,
                    state.client_suppressTerrainScreen,
                    state.client_displayChatNotifications,
                    state.client_meshCacheEnabled,
                    state.client_meshCacheRadius,
                    state.client_meshCacheBudgetMb
            );

            RZeroAdaptiveSettings newAdaptive = new RZeroAdaptiveSettings(
                    state.adaptive_minSaveGapSeconds,
                    state.adaptive_relaxTimeSeconds,
                    state.adaptive_postCombatSavePercent,
                    state.adaptive_saveOnBossDefeat
            );

            RZeroSettings newSettings = RZeroRuntime.settings()
                    .withRzerochashEnabled(state.rzerochashEnabled)
                    .withCheckpointPolicy(newPolicy)
                    .withClientRestore(newClientRestore)
                    .withAdaptive(newAdaptive)
                    .withAnchor(RZeroRuntime.settings().anchor());

            RZeroRuntime.setSettings(newSettings);
            RZeroConfig.save();
        });

        return builder.build();
    }

    private static void addToggle(ConfigCategory category, ConfigEntryBuilder entryBuilder, String key, boolean value, boolean defaultValue, Consumer<Boolean> consumer, boolean hasTooltip) {
        BooleanToggleBuilder builder = entryBuilder.startBooleanToggle(Component.translatable(key), value)
                .setDefaultValue(defaultValue)
                .setSaveConsumer(consumer);
        if (hasTooltip) {
            builder.setTooltip(Component.translatable(key + ".tooltip"));
        }
        category.addEntry(builder.build());
    }

    private static void addSubToggle(SubCategoryBuilder category, ConfigEntryBuilder entryBuilder, String key, boolean value, boolean defaultValue, Consumer<Boolean> consumer) {
        addSubToggle(category, entryBuilder, key, value, defaultValue, consumer, false);
    }

    private static void addSubToggle(SubCategoryBuilder category, ConfigEntryBuilder entryBuilder, String key, boolean value, boolean defaultValue, Consumer<Boolean> consumer, boolean hasTooltip) {
        BooleanToggleBuilder builder = entryBuilder.startBooleanToggle(Component.translatable(key), value)
                .setDefaultValue(defaultValue)
                .setSaveConsumer(consumer);
        if (hasTooltip) {
            builder.setTooltip(Component.translatable(key + ".tooltip"));
        }
        category.add(builder.build());
    }

    private static void addSlider(ConfigCategory category, ConfigEntryBuilder entryBuilder, String key, int value, int min, int max, int defaultValue, Consumer<Integer> consumer, boolean hasTooltip) {
        IntSliderBuilder builder = entryBuilder.startIntSlider(Component.translatable(key), value, min, max)
                .setDefaultValue(defaultValue)
                .setSaveConsumer(consumer);
        if (hasTooltip) {
            builder.setTooltip(Component.translatable(key + ".tooltip"));
        }
        category.addEntry(builder.build());
    }
}