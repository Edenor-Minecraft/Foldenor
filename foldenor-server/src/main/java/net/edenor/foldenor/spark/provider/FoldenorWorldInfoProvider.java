package net.edenor.foldenor.spark.provider;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import me.lucko.spark.paper.common.platform.world.ChunkInfo;
import me.lucko.spark.paper.common.platform.world.WorldInfoProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Server;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;

/** Provides server world information to spark. */
public class FoldenorWorldInfoProvider implements WorldInfoProvider {
    private final Server server;

    public FoldenorWorldInfoProvider() {
        this.server = Bukkit.getServer();
    }

    @Override
    public CountsResult pollCounts() {
        final int players = this.server.getOnlinePlayers().size();
        int entities = 0;
        int chunks = 0;

        final int tileEntities = 0;

        for (final World world : this.server.getWorlds()) {
            entities += world.getEntityCount();
            chunks += world.getChunkCount();
        }

        return new CountsResult(players, entities, tileEntities, chunks);
    }

    @Nullable
    @Override
    public ChunksResult<? extends ChunkInfo<?>> pollChunks() {
        return null;
    }

    @Override
    public GameRulesResult pollGameRules() {
        final GameRulesResult data = new GameRulesResult();
        boolean addDefaults = true;

        for (final World world : this.server.getWorlds()) {
            for (final String gameRule : world.getGameRules()) {
                final GameRule<?> ruleObj = GameRule.getByName(gameRule);
                if (ruleObj == null) {
                    continue;
                }

                if (addDefaults) {
                    //noinspection deprecation
                    data.putDefault(gameRule, Objects.toString(world.getGameRuleDefault(ruleObj)));
                }

                data.put(gameRule, world.getName(), Objects.toString(world.getGameRuleValue(ruleObj)));
            }

            addDefaults = false;
        }

        return data;
    }

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return this.server.getDatapackManager().getEnabledPacks().stream()
            .map(pack -> new DataPackInfo(
                pack.getName(),
                Component.text().append(pack.getDescription()).content(),
                pack.getSource().toString().toLowerCase(Locale.ROOT).replace("_", "")
            )).toList();
    }
}
