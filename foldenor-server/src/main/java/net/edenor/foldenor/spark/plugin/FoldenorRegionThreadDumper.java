package net.edenor.foldenor.spark.plugin;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import me.lucko.spark.paper.common.sampler.ThreadDumper;
import me.lucko.spark.paper.common.util.ThreadFinder;
import me.lucko.spark.paper.proto.SparkSamplerProtos;

/** Selects Folia region tick threads. */
public class FoldenorRegionThreadDumper implements ThreadDumper {
    private final ThreadFinder threadFinder = new ThreadFinder();
    private final Map<Long, Boolean> cache = new HashMap<>();
    private final Pattern foliaThreadNamePattern = Pattern.compile("Folia Region Scheduler Thread #\\d+", Pattern.CASE_INSENSITIVE);

    @Override
    public ThreadInfo[] dumpThreads(final ThreadMXBean threadBean) {
        return this.threadFinder.getThreads()
            .filter((thread) -> this.isThreadIncluded(thread.threadId(), thread.getName()))
            .map((thread) -> threadBean.getThreadInfo(thread.threadId(), Integer.MAX_VALUE))
            .filter(Objects::nonNull)
            .toArray(ThreadInfo[]::new);
    }

    @Override
    public boolean isThreadIncluded(final long threadId, final String threadName) {
        return this.cache.computeIfAbsent(threadId,
            id -> this.foliaThreadNamePattern.matcher(threadName).matches());
    }

    @Override
    public SparkSamplerProtos.SamplerMetadata.ThreadDumper getMetadata() {
        return SparkSamplerProtos.SamplerMetadata.ThreadDumper.newBuilder()
            .setType(SparkSamplerProtos.SamplerMetadata.ThreadDumper.Type.SPECIFIC)
            .build();
    }
}
