package com.invasion.entity;

import java.util.Arrays;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.invasion.InvSounds;

public class ElectricityBoltEntity extends Entity {
    private static final int VERTEX_COUNT = 60;

    private int ticksToRender;
    private final long timeCreated = System.currentTimeMillis();
    private final Vector3f[] vertices = Util.make(new Vector3f[VERTEX_COUNT], v -> Arrays.fill(v, new Vector3f()));
    private long lastVertexUpdate = timeCreated;

    private double distance;
    private float widthVariance = 6;
    private Vec3 ray;
    private boolean soundMade;

    public ElectricityBoltEntity(EntityType<ElectricityBoltEntity> type, Level world) {
        super(type, world);
    }

    public ElectricityBoltEntity(Level world, double x, double y, double z) {
        this(InvEntities.BOLT, world);
        setPos(x, y, z);
    }

    public ElectricityBoltEntity(Level world, Vec3 pos, Vec3 targetPos, int ticksToRender, boolean soundMade) {
        this(InvEntities.BOLT, world);
        setPos(pos);
        ray = targetPos.subtract(pos);
        this.ticksToRender = ticksToRender;
        this.soundMade = soundMade;
        setHeading((float)ray.x, (float)ray.y, (float)ray.z);
        doVertexUpdate();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (++tickCount == 1 && soundMade) {
            playSound(InvSounds.ENTITY_LIGHTNING_ZAP, 1, 1);
        }
        if (tickCount > ticksToRender) {
            discard();
        }
    }

    @Nullable
    public Vector3f[] getVertices() {
        long time = System.currentTimeMillis();
        if (time - timeCreated > ticksToRender * 50) {
            return null;
        }
        if (time - lastVertexUpdate >= 75L) {
            doVertexUpdate();
            while (lastVertexUpdate + 50L <= time) {
                lastVertexUpdate += 50L;
            }
        }
        return vertices;
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 0) {
            playSound(InvSounds.ENTITY_LIGHTNING_ZAP, 1, 1);
        }
    }

    private void setHeading(float x, float y, float z) {
        float xzSq = Mth.square(x) + Mth.square(z);
        setYRot((float)Mth.atan2(x, z) * Mth.RAD_TO_DEG + 90);
        setXRot((float)Mth.atan2(Mth.sqrt(xzSq), y) * Mth.RAD_TO_DEG);
        distance = Math.sqrt(xzSq + Mth.square(y));
    }

    private void doVertexUpdate() {
        widthVariance = (10F / (float) Math.log10(distance + 1));
        for (int vertex = 0; vertex < vertices.length; vertex++) {
            vertices[vertex].y = (vertex * (float)distance / (vertices.length - 1));
        }

        createSegment(0, vertices.length - 1);
    }

    private void createSegment(int begin, int end) {
        int points = end + 1 - begin;
        if (points <= 4) {
            createVertex(begin, begin + 1, end);
            if (points != 3) {
                createVertex(begin, begin + 2, end);
            }
            return;
        }
        int midPoint = begin + points / 2;
        createVertex(begin, midPoint, end);
        createSegment(begin, midPoint);
        createSegment(midPoint, end);
    }

    private void createVertex(int begin, int mid, int end) {
        float xDiff = vertices[end].x - vertices[begin].x;
        float zDiff = vertices[end].z - vertices[begin].z;

        float yDiffToMid = vertices[mid].y - vertices[begin].y;

        float yRatio = yDiffToMid / (vertices[end].y() - vertices[begin].y);

        vertices[mid].x = vertices[begin].x + xDiff * yRatio + (random.nextFloat() - 0.5F) * yDiffToMid * widthVariance;
        vertices[mid].z = vertices[begin].z + zDiff * yRatio + (random.nextFloat() - 0.5F) * yDiffToMid * widthVariance;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}
