package com.invasion.nexus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import com.invasion.util.math.DistanceComparators;
import com.invasion.entity.BoundIMMobRegistry;

public class Combatants implements Iterable<Combatant<?>> {
    private List<Combatant<?>> mobList = new ArrayList<>();
    private boolean sorted;

    private final Nexus nexus;
    private final Comparator<Combatant<?>> sorter;

    public Combatants(Nexus nexus) {
        this.nexus = nexus;
        this.sorter = Comparator.comparing(Combatant::asEntity, DistanceComparators.ofComparisonEntities(com.invasion.util.math.PosUtils.center(nexus.getOrigin())));
    }

    public void updateMobList(AABB arena) {
        mobList = BoundIMMobRegistry.loadedInArena(
                (net.minecraft.server.level.ServerLevel) nexus.getWorld(),
                arena);
        sorted = false;
    }

    @Nullable
    public Combatant<?> removeNearestCombatant() {
        if (mobList.isEmpty()) {
            return null;
        }

        if (!sorted) {
            sorted = true;
            Collections.sort(mobList, sorter);
        }
        return mobList.remove(mobList.size() - 1);
    }

    @Override
    public Iterator<Combatant<?>> iterator() {
        return mobList.iterator();
    }

    public int size() {
        return mobList.size();
    }

    @Nullable
    public Combatant<?> get(int index) {
        return index < mobList.size() ? mobList.get(index) : null;
    }
}
