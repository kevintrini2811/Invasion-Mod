package com.invasion.entity.ai.builder;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.invasion.entity.NexusEntity;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.entity.pathfinding.PathingUtil;

public class TerrainBuilder implements ITerrainBuild {
    private static final float PLANKS_COST = 45;
    private static final float COBBLE_COST = 65;
    private final float DIG_COST = 35;

    private final NexusEntity mob;
    private float buildRate;

    public TerrainBuilder(NexusEntity entity, float buildRate) {
        mob = entity;
        this.buildRate = buildRate;
    }

    public void setBuildRate(float buildRate) {
        this.buildRate = buildRate;
    }

    public float getBuildRate() {
        return this.buildRate;
    }

/**
     * Hilfsfunktion, falls du so etwas noch nicht hast:
     * Nur bestimmte Blöcke dürfen zerstört werden
     * (kein Bedrock, kein Nexus, kein Wasser, etc.).
     */
    // Hilfsfunktion: darf dieser Block in Luft verwandelt werden?
    private boolean canModifyBlock(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // Luft ignorieren
        if (state.isAir()) {
            return false;
        }

        // Unzerstörbare Blöcke (Bedrock etc.) nicht anfassen
        if (state.getDestroySpeed(world, pos) < 0.0F) {
            return false;
        }

        // Wenn du bestimmte Blöcke schützen willst, hier ergänzen
        // z.B. Nexusblock, spezielle Deko, etc.

        return true;
    }

    /**
     * Baut einen senkrechten Schacht nach unten, indem Blöcke zu AIR werden.
     * Es wird eine 2-Blöcke-hohe Luftsäule erzeugt, damit der Mob durchpasst.
     *
     * @param basePos Startposition (z.B. Position des Zombies)
     * @param depth   maximale Tiefe in Blöcken
     */
    public Stream<ModifyBlockEntry> askDigShaftDown(BlockPos basePos, int depth) {
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();
        Level world = mob.asEntity().level();

        // Sicherheitslimit
        int depthClamped = Math.max(1, Math.min(depth, 32));

        // *** WICHTIGER UNTERSCHIED ***
        // Wir nehmen NICHT basePos, sondern IMMER die echte Mob-Position
        BlockPos entityPos = mob.asEntity().blockPosition();
        BlockPos.MutableBlockPos mutable = entityPos.mutable();

        for (int i = 1; i <= depthClamped; i++) {
            // Oberer Block dieses "Segments"
            mutable.set(entityPos).move(Direction.DOWN, i);
            BlockPos pos0 = mutable.immutable();

            // Block direkt darunter -> 2-Blöcke-Höhe
            mutable.move(Direction.DOWN);
            BlockPos pos1 = mutable.immutable();

            if (canModifyBlock(world, pos0)) {
                builder.add(new ModifyBlockEntry(
                        pos0,
                        Blocks.AIR.defaultBlockState(),
                        (int) (DIG_COST / buildRate)
                ));
            }

            if (canModifyBlock(world, pos1)) {
                builder.add(new ModifyBlockEntry(
                        pos1,
                        Blocks.AIR.defaultBlockState(),
                        (int) (DIG_COST / buildRate)
                ));
            }
        }

        return builder.build();
    }

    /**
     * Baut eine schräge Rampe nach oben, indem Blöcke in einer 45°-Linie
     * (vorwärts + nach oben) zu AIR gemacht werden.
     * Es wird jeweils eine 2-Blöcke-hohe "Gänge"-Säule freigeräumt.
     *
     * @param basePos  (wird ignoriert, wir nehmen die Mob-Position)
     * @param dir      horizontale Richtung, in die der Gang gehen soll
     * @param steps    wie viele "Stufen" die Rampe haben soll
     */
    public Stream<ModifyBlockEntry> askBuildRampUp(BlockPos basePos, Direction dir, int steps) {
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();
        Level world = mob.asEntity().level();

        if (!dir.getAxis().isHorizontal()) {
            dir = mob.asEntity().getDirection();
        }

        int clamped = Math.max(1, Math.min(steps, 16));
        BlockPos entityPos = mob.asEntity().blockPosition();
        BlockPos.MutableBlockPos mut = entityPos.mutable();

        for (int i = 1; i <= clamped; i++) {
            // „Stufe“, auf die er treten soll:
            mut.set(entityPos).move(dir, i).move(Direction.UP, i - 1);
            BlockPos stepPos = mut.immutable();

            // Kopf-Freiraum: Block über der Stufe + noch einer drüber
            BlockPos head1 = stepPos.above();
            BlockPos head2 = stepPos.above(2);

            // Stufe platzieren, wenn leer
            if (world.getBlockState(stepPos).isAir()) {
                builder.add(new ModifyBlockEntry(
                        stepPos,
                        Blocks.COBBLESTONE.defaultBlockState(),  // oder Slab/Stair
                        (int) (COBBLE_COST / buildRate)
                ));
            } else if (canModifyBlock(world, stepPos)) {
                // Wenn da schon irgendwas im Weg ist: weg damit und dann unsere Stufe drauf
                builder.add(new ModifyBlockEntry(
                        stepPos,
                        Blocks.COBBLESTONE.defaultBlockState(),
                        (int) (COBBLE_COST / buildRate)
                ));
            }

            // Kopf freimachen
            if (canModifyBlock(world, head1)) {
                builder.add(new ModifyBlockEntry(
                        head1,
                        Blocks.AIR.defaultBlockState(),
                        (int) (DIG_COST / buildRate)
                ));
            }
            if (canModifyBlock(world, head2)) {
                builder.add(new ModifyBlockEntry(
                        head2,
                        Blocks.AIR.defaultBlockState(),
                        (int) (DIG_COST / buildRate)
                ));
            }
        }

        return builder.build();
    }



    @Override
    public Stream<ModifyBlockEntry> askBuildBridge(BlockPos pos) {
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();

        BlockPos.MutableBlockPos mutable = pos.mutable();
        BlockPos posBelow = pos.below();
        Level world = mob.asEntity().level();

        BlockState stateAtFeet = world.getBlockState(pos);
        BlockState stateBelow = world.getBlockState(posBelow);
        boolean fluidAtFeet = !stateAtFeet.getFluidState().isEmpty();
        boolean fluidBelow = !stateBelow.getFluidState().isEmpty();
        boolean isAirBelow = stateBelow.isAir();

        if (isAirBelow || fluidAtFeet || fluidBelow) {
            // In deep water the feet themselves occupy a fluid block. At a
            // normal river edge the feet are in air and the fluid is below.
            BlockPos placementPos = fluidAtFeet ? pos : posBelow;
            boolean needsSupport = IMLandPathNodeMaker.avoidsBlock(mob.asEntity(),
                    mutable.set(placementPos).move(Direction.DOWN))
                    || IMLandPathNodeMaker.avoidsBlock(mob.asEntity(),
                    mutable.set(placementPos).move(Direction.DOWN, 2));
            builder.add(new ModifyBlockEntry(placementPos,
                    (needsSupport ? Blocks.COBBLESTONE : Blocks.OAK_PLANKS).defaultBlockState(),
                    (int) ((needsSupport ? COBBLE_COST : PLANKS_COST) / buildRate))
            );
        }

        return builder.build();
    }

    /**
     * Builds a contiguous bridge run, following the 1.7.10 BRIDGE action:
     * every traversable air node receives a block directly below it. Batching
     * nearby nodes compensates for the modern navigator dropping paths at a
     * void edge, while remaining inside the engineer's build reach.
     */
    public Stream<ModifyBlockEntry> askBuildBridgeLine(
            BlockPos firstPos, Direction direction, int maxLength) {
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();
        Level world = mob.asEntity().level();

        for (int distance = 0; distance < maxLength; distance++) {
            BlockPos feetPos = firstPos.relative(direction, distance);
            BlockState feetState = world.getBlockState(feetPos);
            BlockState headState = world.getBlockState(feetPos.above());
            if ((!PathingUtil.isAirOrReplaceable(feetState)
                    && feetState.getFluidState().isEmpty())
                    || !PathingUtil.isAirOrReplaceable(headState)) {
                break;
            }

            BlockPos floorPos = feetState.getFluidState().isEmpty()
                    ? feetPos.below()
                    : feetPos;
            BlockState floorState = world.getBlockState(floorPos);
            if (floorState.isCollisionShapeFullBlock(world, floorPos)) {
                break;
            }

            BlockPos.MutableBlockPos mutable = floorPos.mutable();
            boolean needsSupport = IMLandPathNodeMaker.avoidsBlock(
                    mob.asEntity(), mutable.move(Direction.DOWN))
                    || IMLandPathNodeMaker.avoidsBlock(
                            mob.asEntity(), mutable.move(Direction.DOWN));
            builder.add(new ModifyBlockEntry(
                    floorPos,
                    (needsSupport ? Blocks.COBBLESTONE : Blocks.OAK_PLANKS)
                            .defaultBlockState(),
                    (int) ((needsSupport ? COBBLE_COST : PLANKS_COST)
                            / buildRate)));
        }

        return builder.build();
    }
}
