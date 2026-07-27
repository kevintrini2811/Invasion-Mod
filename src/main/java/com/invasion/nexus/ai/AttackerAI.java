package com.invasion.nexus.ai;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.CollisionGetter;
import com.invasion.entity.NexusEntity;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.Nexus;
import com.invasion.nexus.ai.scaffold.Scaffold;
import com.invasion.nexus.ai.scaffold.ScaffoldGenerator;
import com.invasion.nexus.ai.scaffold.ScaffoldList;
import com.invasion.nexus.ai.scaffold.ScaffoldView;

public class AttackerAI {
    private static final ExecutorService SCAFFOLD_EXECUTOR = Executors.newSingleThreadExecutor();
    private final Nexus nexus;

    private final Long2ObjectMap<Integer> entityDensityData = new Long2ObjectOpenHashMap<>();

    private final ScaffoldList scaffolds = new ScaffoldList();

    private int nextScaffoldCalcTimer;
    private int updateScaffoldTimer;
    private int nextEntityDensityUpdate;

    public AttackerAI(Nexus nexus) {
        this.nexus = nexus;

    }

    public ScaffoldList getScaffolds() {
        return scaffolds;
    }

    public void tick() {
        nextScaffoldCalcTimer = Math.max(0, nextScaffoldCalcTimer - 1);
        if (--updateScaffoldTimer <= 0) {
            updateScaffoldTimer = 40;
            scaffolds.tick(nexus.getWorld());
        }

        if (--nextEntityDensityUpdate <= 0) {
            nextEntityDensityUpdate = 20;
            entityDensityData.clear();
            for (Combatant<?> mob : nexus.getCombatants()) {
                entityDensityData.compute(mob.asEntity().blockPosition().asLong(), (key, old) -> (old == null ? 1 : old + 1) & ScaffoldView.MOB_DENSITY_FLAG);
            }
        }
    }

    public CollisionGetter wrapEntityData(CollisionGetter terrainMap) {
        return new TerrainDataLayer(terrainMap, entityDensityData);
    }

    public CollisionGetter addScaffoldDataTo(CollisionGetter view) {
        ScaffoldView terrainMap = ScaffoldView.of(view);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Scaffold scaffold : scaffolds) {
            BlockPos pos = scaffold.getNode().pos();
            for (int y = scaffold.getNode().bottom(); y < scaffold.getNode().top(); y++) {
                terrainMap.addScaffoldPosition(mutable.set(pos.getX(), y, pos.getZ()));
            }
        }
        return view;
    }

    public void requestBuildJob(NexusEntity entity, Consumer<Optional<BlockPos>> callback) {
        if (nextScaffoldCalcTimer > 0 || scaffolds.size() > getScaffoldLimit()) {
            callback.accept(Optional.empty());
        } else {
            nextScaffoldCalcTimer = 200;
            boolean success = scaffolds.addAll(nexus, new ScaffoldGenerator(this).generateScaffolds(entity));
            if (success) {
                callback.accept(scaffolds.getNearest(entity.asEntity().blockPosition()));
            } else {
                callback.accept(Optional.empty());
            }
        }
    }

    private int getScaffoldLimit() {
        return 2 + nexus.getCurrentWave() / 2;
    }

    public int getScaffoldSpacing() {
        return 90 / (nexus.getCurrentWave() + 10);
    }

    public void readNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        scaffolds.load(compound.getListOrEmpty("scaffolds")
                .stream()
                .map(element -> new Scaffold((CompoundTag) element, nexus))
                .toList()
        );
    }

    public CompoundTag writeNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        ListTag nbttaglist = new ListTag();
        for (Scaffold scaffold : scaffolds) {
            nbttaglist.add(scaffold.toNBT(new CompoundTag()));
        }
        compound.put("scaffolds", nbttaglist);
        return compound;
    }
}