package com.github.cao.awa.sepals.entity.predicate;

import com.google.common.base.Predicates;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

import java.util.function.Predicate;

public class SepalsEntityPredicates {
    public static Predicate<Entity> quickCanBePushedBy(Entity currentEntity) {
        Team currentTeam = currentEntity.getTeam();
        Team.CollisionRule currentCollisionRule;
        if (currentTeam == null) {
            currentCollisionRule = Team.CollisionRule.ALWAYS;
        } else {
            currentCollisionRule = currentTeam.getCollisionRule();

            if (currentCollisionRule == Team.CollisionRule.NEVER) {
                return Predicates.alwaysFalse();
            }
        }

        boolean currentEntityNoPushOwnTeam = currentCollisionRule != Team.CollisionRule.PUSH_OWN_TEAM;
        boolean currentEntityNoPushOtherTeams = currentCollisionRule != Team.CollisionRule.PUSH_OTHER_TEAMS;

        boolean currentEntityIsClient = currentEntity.level().isClientSide();

        return otherEntity -> {
            if (otherEntity.isSpectator() || !otherEntity.isPushable()) {
                return false;
            }

            if (currentEntityIsClient) {
                if (otherEntity instanceof Player playerEntity) {
                    if (!playerEntity.isLocalPlayer()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            Team otherTeam = otherEntity.getTeam();
            if (otherTeam == null) {
                return currentEntityNoPushOwnTeam && currentEntityNoPushOtherTeams;
            } else {
                Team.CollisionRule otherCollisionRule = otherTeam.getCollisionRule();
                if (otherTeam.isAlliedTo(currentTeam)) {
                    boolean otherEntityNoPushOwnTeam = otherCollisionRule != Team.CollisionRule.PUSH_OWN_TEAM;
                    return currentEntityNoPushOwnTeam && otherEntityNoPushOwnTeam;
                } else {
                    boolean otherEntityNoPushOtherTeams = otherCollisionRule != Team.CollisionRule.PUSH_OTHER_TEAMS;

                    return currentEntityNoPushOtherTeams && otherEntityNoPushOtherTeams;
                }
            }
        };
    }
}