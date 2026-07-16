package net.edenor.foldenor.spark;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import me.lucko.spark.api.Spark;
import me.lucko.spark.paper.PaperCommandSender;
import me.lucko.spark.paper.api.PaperClassLookup;
import me.lucko.spark.paper.api.PaperScheduler;
import me.lucko.spark.paper.api.PaperSparkModule;
import me.lucko.spark.paper.common.SparkPlatform;
import me.lucko.spark.paper.common.SparkPlugin;
import me.lucko.spark.paper.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.paper.common.monitor.tick.TickStatistics;
import me.lucko.spark.paper.common.platform.PlatformInfo;
import me.lucko.spark.paper.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.paper.common.platform.world.WorldInfoProvider;
import me.lucko.spark.paper.common.sampler.ThreadDumper;
import me.lucko.spark.paper.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.paper.common.sampler.source.SourceMetadata;
import me.lucko.spark.paper.common.tick.TickHook;
import me.lucko.spark.paper.common.tick.TickReporter;
import me.lucko.spark.paper.common.util.classfinder.ClassFinder;
import net.edenor.foldenor.spark.plugin.FoldenorRegionThreadDumper;
import net.edenor.foldenor.spark.plugin.FoldenorTickStatistics;
import net.edenor.foldenor.spark.provider.FoldenorClassSourceLookup;
import net.edenor.foldenor.spark.provider.FoldenorPlayerPingProvider;
import net.edenor.foldenor.spark.provider.FoldenorServerConfigProvider;
import net.edenor.foldenor.spark.provider.FoldenorWorldInfoProvider;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class FoldenorSparkPlugin implements PaperSparkModule, SparkPlugin {
    private final Server server;
    private final Logger logger;
    private final PaperScheduler scheduler;
    private final PaperClassLookup classLookup;
    private final ThreadDumper gameThreadDumper;
    private final SparkPlatform platform;

    private FoldenorSparkPlugin(
        final Server server, final Logger logger, final PaperScheduler scheduler, final PaperClassLookup classLookup
    ) {
        this.server = server;
        this.logger = logger;
        this.scheduler = scheduler;
        this.classLookup = classLookup;
        this.gameThreadDumper = new FoldenorRegionThreadDumper();
        this.platform = new SparkPlatform(this);
    }

    public static PaperSparkModule create(
        final Server server, final Logger logger, final PaperScheduler scheduler, final PaperClassLookup classLookup
    ) {
        return new FoldenorSparkPlugin(server, logger, scheduler, classLookup);
    }

    @Override
    public void enable() {
        this.platform.enable();
    }

    @Override
    public void disable() {
        this.platform.disable();
    }

    @Override
    public void executeCommand(final CommandSender sender, final String[] args) {
        this.platform.executeCommand(new PaperCommandSender(sender), args);
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String[] args) {
        return this.platform.tabCompleteCommand(new PaperCommandSender(sender), args);
    }

    @Override
    public boolean hasPermission(final CommandSender sender) {
        return this.platform.hasPermissionForAnyCommand(new PaperCommandSender(sender));
    }

    @Override
    public Collection<String> getPermissions() {
        return this.platform.getAllSparkPermissions();
    }

    @Override
    public void onServerTickStart() {
    }

    @Override
    public void onServerTickEnd(double duration) {
    }

    @Override
    public String getVersion() {
        return "1.10.152";
    }

    @Override
    public Path getPluginDirectory() {
        return this.server.getPluginsFolder().toPath().resolve("spark");
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<PaperCommandSender> getCommandSenders() {
        return Stream.concat(this.server.getOnlinePlayers().stream(), Stream.of(this.server.getConsoleSender())).map(PaperCommandSender::new);
    }

    @Override
    public void executeAsync(final Runnable task) {
        this.scheduler.executeAsync(task);
    }

    @Override
    public void executeSync(final Runnable task) {
        this.scheduler.executeSync(task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public TickHook createTickHook() {
        return null;
    }

    @Override
    public TickReporter createTickReporter() {
        return null;
    }

    @Override
    public TickStatistics createTickStatistics() {
        return new FoldenorTickStatistics();
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new FoldenorClassSourceLookup();
    }

    @Override
    public ClassFinder createClassFinder() {
        return (className) -> {
            try {
                return this.classLookup.lookup(className);
            } catch (final Throwable ignored) {
                return null;
            }
        };
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
            Arrays.asList(this.server.getPluginManager().getPlugins()),
            Plugin::getName,
            (plugin) -> plugin.getPluginMeta().getVersion(),
            (plugin) -> String.join(", ", plugin.getPluginMeta().getAuthors()),
            (plugin) -> plugin.getPluginMeta().getDescription()
        );
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new FoldenorPlayerPingProvider(this.server);
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new FoldenorServerConfigProvider();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new FoldenorWorldInfoProvider();
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new FoldenorPlatformInfo(this.server);
    }

    @Override
    public void registerApi(final Spark api) {
        // no-op
    }

    @Override
    public void log(final Level level, final String msg) {
        this.logger.log(level, msg);
    }

    @Override
    public void log(final Level level, final String msg, final Throwable throwable) {
        this.logger.log(level, msg, throwable);
    }
}
