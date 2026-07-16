package net.edenor.foldenor.spark.plugin;

import ca.spottedleaf.moonrise.common.time.TickData;
import io.papermc.paper.threadedregions.RegionizedServer;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.TickRegionScheduler;
import io.papermc.paper.threadedregions.TickRegions;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.function.Consumer;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.paper.common.monitor.tick.TickStatistics;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

/** Reads TPS from Folia region tick data. */
public class FoldenorTickStatistics implements TickStatistics {
    private static final double DEFAULT_TPS = 20.0;
    private static final AverageInfo EMPTY_AVERAGE_INFO = new AverageInfo();

    @Override
    public double tps5Sec() {
        return getTpsFor(TimeSpan.TPS_5_SEC);
    }

    @Override
    public double tps10Sec() {
        // Folia has no 10 second bucket.
        return getTpsFor(TimeSpan.TPS_15_SEC);
    }

    @Override
    public double tps1Min() {
        return getTpsFor(TimeSpan.TPS_1_MIN);
    }

    @Override
    public double tps5Min() {
        return getTpsFor(TimeSpan.TPS_5_MIN);
    }

    @Override
    public double tps15Min() {
        return getTpsFor(TimeSpan.TPS_15_MIN);
    }

    @Override
    public boolean isDurationSupported() {
        return false;
    }

    @Override
    public DoubleAverageInfo duration10Sec() {
        return EMPTY_AVERAGE_INFO;
    }

    @Override
    public DoubleAverageInfo duration1Min() {
        return EMPTY_AVERAGE_INFO;
    }

    @Override
    public DoubleAverageInfo duration5Min() {
        return EMPTY_AVERAGE_INFO;
    }

    private static double getTpsFor(final TimeSpan span) {
        final long now = System.nanoTime();
        final DoubleArrayList tpsCounts = new DoubleArrayList();

        final Consumer<ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData>> forEachRegion =
            (region) -> tpsCounts.add(getTpsFor(region.getData().getRegionSchedulingHandle(), span, now));

        tpsCounts.add(getTpsFor(RegionizedServer.getGlobalTickData(), span, now));

        for (final World world : Bukkit.getServer().getWorlds()) {
            final ServerLevel level = ((CraftWorld) world).getHandle();
            level.regioniser.computeForAllRegionsUnsynchronised(forEachRegion);
        }

        if (tpsCounts.isEmpty()) {
            return DEFAULT_TPS;
        }

        return tpsCounts.doubleStream().sum() / tpsCounts.size();
    }

    private static double getTpsFor(final TickRegionScheduler.RegionScheduleHandle handle, final TimeSpan span, final long now) {
        if (handle == null) {
            return DEFAULT_TPS;
        }
        final TickData.TickReportData report = switch (span) {
            case TPS_5_SEC -> handle.getTickReport5s(now);
            case TPS_15_SEC -> handle.getTickReport15s(now);
            case TPS_1_MIN -> handle.getTickReport1m(now);
            case TPS_5_MIN -> handle.getTickReport5m(now);
            case TPS_15_MIN -> handle.getTickReport15m(now);
        };
        return report == null ? DEFAULT_TPS : report.tpsData().segmentAll().average();
    }

    private enum TimeSpan {
        TPS_5_SEC,
        TPS_15_SEC,
        TPS_1_MIN,
        TPS_5_MIN,
        TPS_15_MIN
    }

    private static final class AverageInfo implements DoubleAverageInfo {

        @Override
        public double mean() {
            return 0;
        }

        @Override
        public double max() {
            return 0;
        }

        @Override
        public double min() {
            return 0;
        }

        @Override
        public double median() {
            return DoubleAverageInfo.super.median();
        }

        @Override
        public double percentile95th() {
            return DoubleAverageInfo.super.percentile95th();
        }

        @Override
        public double percentile(final double v) {
            return 0;
        }
    }
}
