package net.edenor.foldenor.config;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.triassic.linearpaper.region.RegionFileFormat;
import dev.kaiijumc.kaiiju.KaiijuEntityLimits;
import io.canvasmc.canvas.simd.SIMDDetection;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FoldenorConfig {
    public static ComponentLogger LOGGER = ComponentLogger.logger("Foldenor");
    protected static final String HEADER = "This is the main configuration file for Foldenor.";
    public static YamlConfiguration config;
    public static int version;
    public static boolean sendNullEntityPackets = false;
    public static boolean appleskinProtocol = false;
    public static int appleskinSyncTickInterval = 20;
    public static boolean useAlternateKeepAlive = false;
    public static int piglinSpawnChancePersentInPortal = 100;
    public static boolean useVirtualThreadForAsyncScheduler = false;
    public static boolean skipMapItemUpdatesIfNoBukkitRender = true;
    public static int entityActivationCheckFrequency = 20;
    public static int linearFlushFrequency = 10;
    public static int linearFlushThreads = 1;
    public static RegionFileFormat regionFormat = RegionFileFormat.ANVIL;
    public static int linearCompressionLevel = 1;
    public static boolean dearEnabled;
    public static int startDistance;
    public static int startDistanceSquared;
    public static int maximumActivationPrio;
    public static int activationDistanceMod;
    public static boolean dontEnableIfInWater = false;
    public static boolean skipAIForNonAwareMob = true;
    public static boolean throttleHopperWhenFullEnabled = false;
    public static int throttleHopperWhenFullSkipTicks = 0;
    public static boolean asyncPlayerDataSaveEnabled = false;
    public static boolean villagersDontReleaseMemoryFix = true;
    public static boolean skipSecondaryPOISensorIfAbsent = false;
    public static boolean turtleEggSearch = true;
    public static float turtleEggSearchPercentChance = 100;
    public static boolean mobRePathFinding = true;
    public static boolean lmdEnabled = false;
    public static Set<String> mobNames = new HashSet<>(List.of("corpse:corpse")); // Let Me Despawn
    public static boolean cacheBiomeEnabled = false;
    public static boolean cacheBiomeMobSpawn = false;
    public static boolean cacheBiomeAdvancement = false;
    public static boolean optimizePlayerMovementProcessing = true;
    public static boolean asyncProtocolChange = false;
    public static boolean foliaPOIAccessOffRegionFix = false;
    public static boolean forceCleanupEntityBrainMemoryForEntity = false;
    public static boolean forceCleanupEntityBrainMemoryForBlockPos = false;
    public static int checkNearbyItemHopperInterval = 1;
    public static int checkNearbyItemMinecartHopperInterval = 1;
    public static int checkTemporaryImmunityDuration = 100;
    public static int checkTemporaryImmunityItemMaxAge = 1200;
    public static int checkTemporaryImmunityCheckForMinecartNearItemInterval = 20;
    public static boolean checkTemporaryImmunityCheckForMinecartNearItemWhileActive = false;
    public static boolean checkTemporaryImmunityCheckForMinecartNearItemWhileInactive = true;
    public static float checkTemporaryImmunityMaxItemHorizontalDist = 24.0f;
    public static float checkTemporaryImmunityMaxItemVerticalDist = 24.0f;
    public static boolean enableSepalsLivingTargetCache = false;
    public static boolean enableSepalsLivingTargetCacheQuickSort = false;
    public static boolean enableSepalsEntitiesCramming = false;
    public static boolean enableSepalsQuickCanBePushByEntityPredicate = false;
    public static boolean enableSepalsBrain = false;
    public static boolean enableSepalsVillagers = false;
    protected static File CONFIG_FILE;
    static boolean verbose;

    public static void init(File configFile) {
        init(configFile, true);
    }

    public static void reload(File configFile) {
        init(configFile, false);
    }

    private static void init(File configFile, boolean setup) {
        CONFIG_FILE = configFile;
        config = new YamlConfiguration();
        if (configFile.exists()) {
            try {
                config.load(CONFIG_FILE);
            } catch (InvalidConfigurationException ex) {
                LOGGER.error("Could not load foldenor.yml, please correct your syntax errors", ex);
                throw Throwables.propagate(ex);
            } catch (IOException ignore) {
            }
        }
        config.options().header(HEADER);
        config.options().copyDefaults(true);
        verbose = getBoolean("verbose", false);

        version = getInt("config-version", 1);
        set("config-version", 1);

        readConfig();
    }

    static void readConfig() {
        readNetworkSettings();

        readOptimizationSettings();

        readMiscSettings();

        readLinearRegion();

        KaiijuEntityLimits.init();

        try {
            dynamicActivationOfBrains();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // SIMD


        try {
            SIMDDetection.isEnabled = SIMDDetection.canEnable(LOGGER);
        } catch (NoClassDefFoundError | Exception ignored) {
            ignored.printStackTrace();
        }


        if (SIMDDetection.isEnabled) {
            LOGGER.info("SIMD operations detected as functional. Will replace some operations with faster versions.");
        } else {
            LOGGER.warn("SIMD operations are available for your server, but are not configured!");
            LOGGER.warn("To enable additional optimizations, add \"--add-modules=jdk.incubator.vector\" to your startup flags, BEFORE the \"-jar\".");
            LOGGER.warn("If you have already added this flag, then SIMD operations are not supported on your JVM or CPU.");
            LOGGER.warn("Debug: Java: {}, test run: {}", System.getProperty("java.version"), SIMDDetection.testRun);
        }

        try {
            config.save(CONFIG_FILE);
        } catch (IOException ex) {
            LOGGER.error("Could not save {}", CONFIG_FILE, ex);
        }
    }

    private static void readLinearRegion() {
        linearFlushFrequency = getInt("region-format.linear.flush-frequency", linearFlushFrequency);
        linearFlushThreads = getInt("region-format.linear.flush-max-threads", linearFlushThreads);

        if (linearFlushThreads < 0)
            linearFlushThreads = Math.max(Runtime.getRuntime().availableProcessors() + linearFlushThreads, 1);
        else
            linearFlushThreads = Math.max(linearFlushThreads, 1);

        regionFormat = RegionFileFormat.fromString(getString("region-format.type", regionFormat.name()));
        if (regionFormat.equals(RegionFileFormat.INVALID)) {
            LOGGER.error("Unknown region format in linear.yml: {}", regionFormat);
            LOGGER.error("Falling back to ANVIL region file format.");
            regionFormat = RegionFileFormat.ANVIL;
        }

        linearCompressionLevel = getInt("region-format.linear.compression-level", linearCompressionLevel);
        if (linearCompressionLevel > 23 || linearCompressionLevel < 1) {
            LOGGER.error("Linear region compression level should be between 1 and 22 in linear.yml: {}", linearCompressionLevel);
            LOGGER.error("Falling back to compression level 1.");
            linearCompressionLevel = 1;
        }
    }

    protected static void readNetworkSettings() {
        sendNullEntityPackets = getBoolean("network.send-null-entity-packets", sendNullEntityPackets);
        appleskinProtocol = getBoolean("network.appleskin-protocol", appleskinProtocol);
        useAlternateKeepAlive = getBoolean("network.use-alternate-keepalive", useAlternateKeepAlive);
        appleskinSyncTickInterval = getInt("network.appleskin-sync-tick-interval", appleskinSyncTickInterval);
        asyncProtocolChange = getBoolean("network.async-protocol-change", useAlternateKeepAlive);
    }

    private static void readOptimizationSettings() {
        piglinSpawnChancePersentInPortal = getInt("optimizations.piglin-spawn-chance-persent-in-portal", 100,
                "Reduces piglin spawn in portal, by reducing change to spawn");
        skipMapItemUpdatesIfNoBukkitRender = getBoolean("optimizations.skip_map_item_updates_if_no_bukkit_render", skipMapItemUpdatesIfNoBukkitRender);
        entityActivationCheckFrequency = getInt("optimizations.entity-activation-check-frequency", 20);
        skipAIForNonAwareMob = getBoolean("optimizations.skip-ai-for-non-aware-mob", skipAIForNonAwareMob);
        throttleHopperWhenFullEnabled = getBoolean("optimizations.throttle-hopper-when-full.enabled", throttleHopperWhenFullEnabled);
        throttleHopperWhenFullSkipTicks = getInt("optimizations.throttle-hopper-when-full.skip-ticks", throttleHopperWhenFullSkipTicks);
        villagersDontReleaseMemoryFix = getBoolean("optimizations.villagers-dont-release-memory-fix", villagersDontReleaseMemoryFix);
        skipSecondaryPOISensorIfAbsent = getBoolean("optimizations.skip-secondary-POI-sensor-if-absent", skipSecondaryPOISensorIfAbsent);
        turtleEggSearch = getBoolean("optimizations.turtle-egg-search.enabled", turtleEggSearch);
        turtleEggSearchPercentChance = (float) getDouble("optimizations.turtle-egg-search.enabled", turtleEggSearchPercentChance);
        mobRePathFinding = getBoolean("optimizations.mob-repathfinding", mobRePathFinding);
        lmdEnabled = getBoolean("optimizations.lmd.enabled", lmdEnabled);
        mobNames = getLMDMobList();
        cacheBiomeEnabled = getBoolean("optimizations.cache-biome.enabled", cacheBiomeEnabled);
        cacheBiomeMobSpawn = getBoolean("optimizations.cache-biome.mob-spawn", cacheBiomeMobSpawn);
        cacheBiomeAdvancement = getBoolean("optimizations.cache-biome.advancement", cacheBiomeAdvancement);
        optimizePlayerMovementProcessing = getBoolean("optimizations.optimize-player-movement-processing", optimizePlayerMovementProcessing);
        forceCleanupEntityBrainMemoryForEntity = getBoolean("optimizations.force-cleanup-entity-brain-memory.for-entity", forceCleanupEntityBrainMemoryForEntity);
        forceCleanupEntityBrainMemoryForBlockPos = getBoolean("optimizations.force-cleanup-entity-brain-memory.for-block-pos", forceCleanupEntityBrainMemoryForBlockPos);
        checkNearbyItemHopperInterval = getInt("optimizations.check-nearby-item.hopper.interval", checkNearbyItemHopperInterval);
        checkNearbyItemMinecartHopperInterval = getInt("optimizations.check-nearby-item.minecart.interval", checkNearbyItemMinecartHopperInterval);
        checkTemporaryImmunityDuration = getInt("optimizations.check-nearby-item.minecart.immunity.duration", checkTemporaryImmunityDuration);
        checkTemporaryImmunityItemMaxAge = getInt("optimizations.check-nearby-item.minecart.immunity.item-max-age", checkTemporaryImmunityItemMaxAge);
        checkTemporaryImmunityCheckForMinecartNearItemInterval = getInt("optimizations.check-nearby-item.minecart.immunity.check-for-minecart-near-item-interval", checkTemporaryImmunityCheckForMinecartNearItemInterval);
        checkTemporaryImmunityCheckForMinecartNearItemWhileActive = getBoolean("optimizations.check-nearby-item.minecart.immunity.check-for-minecart-near-item-while-active", checkTemporaryImmunityCheckForMinecartNearItemWhileActive);
        checkTemporaryImmunityCheckForMinecartNearItemWhileInactive = getBoolean("optimizations.check-nearby-item.minecart.immunity.check-for-minecart-near-item-while-inactive", checkTemporaryImmunityCheckForMinecartNearItemWhileInactive);
        checkTemporaryImmunityMaxItemHorizontalDist = (float) getDouble("optimizations.check-nearby-item.minecart.immunity.max-item-horizontal-distance", checkTemporaryImmunityMaxItemHorizontalDist);
        checkTemporaryImmunityMaxItemVerticalDist = (float) getDouble("optimizations.check-nearby-item.minecart.immunity.max-item-vertical-distance", checkTemporaryImmunityMaxItemVerticalDist);
        enableSepalsLivingTargetCache = getBoolean("optimizations.sepals.enableSepalsLivingTargetCache", false);
        enableSepalsLivingTargetCacheQuickSort = getBoolean("optimizations.sepals.enableSepalsLivingTargetCacheQuickSort", false);
        enableSepalsEntitiesCramming = getBoolean("optimizations.sepals.enableSepalsEntitiesCramming", false);
        enableSepalsQuickCanBePushByEntityPredicate = getBoolean("optimizations.sepals.enableSepalsQuickCanBePushByEntityPredicate", false);
        enableSepalsBrain = getBoolean("optimizations.sepals.enableSepalsBrain", false);
        enableSepalsVillagers = getBoolean("optimizations.sepals.enableSepalsVillagers", false);
    }

    private static void readMiscSettings() {
        useVirtualThreadForAsyncScheduler = getBoolean("optimizations.use-virtual-thread-for-async-scheduler", useVirtualThreadForAsyncScheduler,
                "Use the new Virtual Thread introduced in JDK 21 for CraftAsyncScheduler.");
        asyncPlayerDataSaveEnabled = getBoolean("misc.async-playerdata-save.enabled", asyncPlayerDataSaveEnabled);
        foliaPOIAccessOffRegionFix = getBoolean("misc.folia-POI-access-off-region-fix", foliaPOIAccessOffRegionFix);
    }

    private static void dynamicActivationOfBrains() throws IOException {
        dearEnabled = getBoolean("dab.enabled", true);
        startDistance = getInt("dab.start-distance", 12,
                "This value determines how far away an entity has to be",
                "from the player to start being effected by DEAR.");
        startDistanceSquared = startDistance * startDistance;
        maximumActivationPrio = getInt("dab.max-tick-freq", 20,
                "This value defines how often in ticks, the furthest entity",
                "will get their pathfinders and behaviors ticked. 20 = 1s");
        activationDistanceMod = getInt("dab.activation-dist-mod", 8,
                "This value defines how much distance modifies an entity's",
                "tick frequency. freq = (distanceToPlayer^2) / (2^value)",
                "If you want further away entities to tick less often, use 7.",
                "If you want further away entities to tick more often, try 9.");
        dontEnableIfInWater = getBoolean("dab.dont-enable-if-in-water", dontEnableIfInWater);

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            entityType.dabEnabled = true; // reset all, before setting the ones to true
        }
        getStringList("dab.blacklisted-entities", Collections.emptyList(), "A list of entities to ignore for activation")
                .forEach(name -> EntityType.byString(name).ifPresentOrElse(entityType -> {
                    entityType.dabEnabled = false;
                }, () -> MinecraftServer.LOGGER.warn("Unknown entity \"" + name + "\"")));
    }

    protected static void set(String path, Object val) {
        config.addDefault(path, val);
        config.set(path, val);
    }

    protected static Set<String> getLMDMobList()
    {
        List<String> fromCfg = getList("optimizations.lmd.mob-names", new ArrayList<>());
        if (!fromCfg.isEmpty())
            mobNames = new HashSet<>(fromCfg);
        return mobNames;
    }

    protected static String getString(String path, String def, String... comment) {
        config.addDefault(path, def);
        return config.getString(path, config.getString(path));
    }

    protected static boolean getBoolean(String path, boolean def, String... comment) {
        config.addDefault(path, def);
        return config.getBoolean(path, config.getBoolean(path));
    }

    protected static double getDouble(String path, double def, String... comment) {
        config.addDefault(path, def);
        return config.getDouble(path, config.getDouble(path));
    }

    protected static int getInt(String path, int def, String... comment) {
        config.addDefault(path, def);
        return config.getInt(path, config.getInt(path));
    }

    protected static <T> List getList(String path, T def, String... comment) {
        config.addDefault(path, def);
        return config.getList(path, config.getList(path));
    }

    protected static List<String> getStringList(String path, List<String> def, String... comment) {
        config.addDefault(path, def);
        return config.getStringList(path);
    }

    static Map<String, Object> getMap(String path, Map<String, Object> def, String... comment) {
        if (def != null && config.getConfigurationSection(path) == null) {
            config.addDefault(path, def);
            return def;
        }
        return toMap(config.getConfigurationSection(path));
    }

    protected static String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, config.getString(path));
    }

    protected static boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, config.getBoolean(path));
    }

    protected static double getDouble(String path, double def) {
        config.addDefault(path, def);
        return config.getDouble(path, config.getDouble(path));
    }

    protected static int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt(path, config.getInt(path));
    }

    protected static <T> List getList(String path, T def) {
        config.addDefault(path, def);
        return config.getList(path, config.getList(path));
    }

    static Map<String, Object> getMap(String path, Map<String, Object> def) {
        if (def != null && config.getConfigurationSection(path) == null) {
            config.addDefault(path, def);
            return def;
        }
        return toMap(config.getConfigurationSection(path));
    }

    protected static Map<String, Object> toMap(ConfigurationSection section) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object obj = section.get(key);
                if (obj != null) {
                    builder.put(key, obj instanceof ConfigurationSection val ? toMap(val) : obj);
                }
            }
        }
        return builder.build();
    }
}