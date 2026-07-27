package com.invasion.nexus;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.invasion.InvasionMod;

public interface Combatant<T extends LivingEntity> extends IHasNexus {
    Predicate<Entity> PREDICATE = EntitySelector.LIVING_ENTITY_STILL_ALIVE.and(EntitySelector.NO_CREATIVE_OR_SPECTATOR).and(i -> i instanceof Combatant);

    @Deprecated
    String getLegacyName();

    T asEntity();

    default void resetHealth() {
        T self = asEntity();
        float health = InvasionMod.getConfig().getHealth(this);
        self.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        self.setHealth(health);
    }

    @Override
    default double findDistanceToNexus() {
        if (!hasNexus()) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(asEntity().distanceToSqr(com.invasion.util.math.PosUtils.center(getNexus().getOrigin())));
    }
}
