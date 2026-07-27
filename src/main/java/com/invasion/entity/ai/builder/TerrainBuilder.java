package com.invasion.entity.ai.builder;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import com.invasion.entity.NexusEntity;
import com.invasion.entity.pathfinding.ClimberUtil;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.nexus.ai.scaffold.Scaffold;
import com.invasion.util.math.PosUtils;

public class TerrainBuilder implements ITerrainBuild {
    private static final float LADDER_COST = 25;
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

    @Override
    public Stream<ModifyBlockEntry> askBuildScaffoldLayer(BlockPos pos) {
        @Nullable
        Scaffold scaffold = mob.getNexus().getAttackerAI().getScaffolds().getAt(pos);
        if (scaffold == null) {
            return Stream.empty();
        }

        int height = pos.getY() - scaffold.getNode().bottom();
        Direction offset = scaffold.getNode().orientation();
        BlockPos.MutableBlockPos mutable = pos.mutable();

        Level world = mob.asEntity().level();
        BlockState block = world.getBlockState(mutable.set(pos).move(offset).move(Direction.DOWN));
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();

        // NEU: Ladder-State mit richtigem Facing
        BlockState ladderState = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, offset.getOpposite());


        // Unterste Ebene direkt über Boden / Einstieg
        if (height == 1) {
            // Block vor der Leiter (Boden) mit Planks auffüllen
            if (!block.isCollisionShapeFullBlock(world, mutable)) {
                builder.add(new ModifyBlockEntry(
                        mutable.immutable(),
                        Blocks.OAK_PLANKS.defaultBlockState(),
                        (int) (PLANKS_COST / buildRate)
                ));
            }

            // Leiter unterhalb der aktuellen Position setzen,
            // aber nur wenn da überhaupt eine Leiter dran halten kann
            mutable.set(pos).move(Direction.DOWN);
            if (world.isEmptyBlock(mutable)
                    && ClimberUtil.canPositionSupportLadder(world, mutable, offset)) {
                builder.add(new ModifyBlockEntry(
                        mutable.immutable(),
                        ladderState,
                        (int) (LADDER_COST / buildRate)
                ));
            }
        }

        // Block vor der Leiter (Scaffold-Wand) auf dieser Höhe schließen
        mutable.set(pos).move(offset);
        if (!world.getBlockState(mutable).isCollisionShapeFullBlock(world, mutable)) {
            builder.add(new ModifyBlockEntry(
                    mutable.immutable(),
                    Blocks.OAK_PLANKS.defaultBlockState(),
                    (int) (PLANKS_COST / buildRate)
            ));
        }

        // Leiter auf der aktuellen Scaffold-Position
        mutable.set(pos);
        if (!world.getBlockState(mutable).is(Blocks.LADDER)
                && ClimberUtil.canPositionSupportLadder(world, mutable, offset)) {
            builder.add(new ModifyBlockEntry(
                    mutable.immutable(),
                    ladderState,
                    (int) (LADDER_COST / buildRate)
            ));
        }

        // Plattform-Layer: Ring aus Planks um die Leiter herum
        if (scaffold.isPlatformLayer(height)) {
            for (Vec3i i : PosUtils.OFFSET_RING) {
                if (!i.equals(offset.getUnitVec3i())) {
                    mutable.set(pos).move(i);
                    if (!world.getBlockState(mutable).isCollisionShapeFullBlock(world, mutable)) {
                        builder.add(new ModifyBlockEntry(
                                mutable.immutable(),
                                Blocks.OAK_PLANKS.defaultBlockState(),
                                (int) (PLANKS_COST / buildRate)
                        ));
                    }
                }
            }
        }

        return builder.build();
    }


    @Override
    public Stream<ModifyBlockEntry> askBuildLadderTower(BlockPos basePos, Direction orientation, int layersToBuild) {
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();
        Level world = mob.asEntity().level();

        if (!orientation.getAxis().isHorizontal()) {
            orientation = mob.asEntity().getDirection();
        }

        BlockState ladderState = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, orientation.getOpposite());

        BlockPos.MutableBlockPos mutable = basePos.mutable();

        // Wenn layersToBuild zu klein ist (oder 0), bau wenigstens 6 hoch,
        // damit man den Effekt deutlich sieht.
        int height = Math.max(layersToBuild, 6);

        for (int i = 0; i < height; i++) {
            // Position der Leiter
            mutable.set(basePos).move(Direction.UP, i);
            BlockPos ladderPos = mutable.immutable();

            // Support dahinter
            BlockPos supportPos = ladderPos.relative(orientation);

            if (!world.getBlockState(supportPos).isCollisionShapeFullBlock(world, mutable.set(supportPos))) {
                builder.add(new ModifyBlockEntry(
                        supportPos,
                        Blocks.OAK_PLANKS.defaultBlockState(),
                        (int) (PLANKS_COST / buildRate)
                ));
            }

            // Leiter setzen
            builder.add(new ModifyBlockEntry(
                    ladderPos,
                    ladderState,
                    (int) (LADDER_COST / buildRate)
            ));
        }

        return builder.build();
    }


    public Stream<ModifyBlockEntry> askBuildLadderShaftDown(BlockPos basePos, Direction orientation, int depth) {
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();
        Level world = mob.asEntity().level();

        // Orientierung sicherstellen
        if (!orientation.getAxis().isHorizontal()) {
            orientation = mob.asEntity().getDirection();
        }

        BlockState ladderState = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, orientation.getOpposite());

        BlockPos.MutableBlockPos mutable = basePos.mutable();

        // Tiefe begrenzen
        int depthClamped = Math.max(1, Math.min(depth, 32));

        for (int i = 1; i <= depthClamped; i++) {
            // Leiter-Position nach unten
            mutable.set(basePos).move(Direction.DOWN, i);
            BlockPos ladderPos = mutable.immutable();

            // Support-Block hinter der Leiter
            BlockPos supportPos = ladderPos.relative(orientation);
            if (!world.getBlockState(supportPos).isCollisionShapeFullBlock(world, mutable.set(supportPos))) {
                builder.add(new ModifyBlockEntry(
                        supportPos,
                        Blocks.OAK_PLANKS.defaultBlockState(),
                        (int) (PLANKS_COST / buildRate)
                ));
            }

            // Leiter selbst
            builder.add(new ModifyBlockEntry(
                    ladderPos,
                    ladderState,
                    (int) (LADDER_COST / buildRate)
            ));
        }

        return builder.build();
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
    public Stream<ModifyBlockEntry> askBuildLadder(BlockPos pos, Direction orientation) {
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();
        Level world = mob.asEntity().level();
        BlockPos.MutableBlockPos mutable = pos.mutable();

        // Leiter nach vorn ausgerichtet
        BlockState ladderState = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, orientation.getOpposite());

        // Wir bauen pauschal 4 Blöcke hoch (kannst du später erhöhen)
        int height = 4;

        for (int i = 0; i < height; i++) {
            // Position der Leiter
            mutable.set(pos).move(Direction.UP, i);
            BlockPos ladderPos = mutable.immutable();

            // Block HINTER der Leiter (Support)
            BlockPos supportPos = ladderPos.relative(orientation);

            // Support immer aus Planks setzen, wenn nicht voll
            if (!world.getBlockState(supportPos).isCollisionShapeFullBlock(world, mutable.set(supportPos))) {
                builder.add(new ModifyBlockEntry(
                        supportPos,
                        Blocks.OAK_PLANKS.defaultBlockState(),
                        (int) (PLANKS_COST / buildRate)
                ));
            }

            // Leiter selbst setzen – ohne irgendwelche Checks
            builder.add(new ModifyBlockEntry(
                    ladderPos,
                    ladderState,
                    (int) (LADDER_COST / buildRate)
            ));
        }

        return builder.build();
    }


    @Override
    public Stream<ModifyBlockEntry> askBuildBridge(BlockPos pos) {
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();

        BlockPos.MutableBlockPos mutable = pos.mutable();
        BlockPos posBelow = pos.below();
        Level world = mob.asEntity().level();

        boolean isFluid = world.getBlockState(pos).liquid();
        boolean isAirBelow = world.isEmptyBlock(posBelow);

        if (isAirBelow || isFluid) {
            if (isFluid) {
                posBelow = pos;
            }
            boolean needsSupport = IMLandPathNodeMaker.avoidsBlock(mob.asEntity(), mutable.set(pos).move(Direction.DOWN, 2))
                                || IMLandPathNodeMaker.avoidsBlock(mob.asEntity(), mutable.set(pos).move(Direction.DOWN, 3));
            builder.add(new ModifyBlockEntry(posBelow,
                    (needsSupport ? Blocks.COBBLESTONE : Blocks.OAK_PLANKS).defaultBlockState(),
                    (int) ((needsSupport ? COBBLE_COST : PLANKS_COST) / buildRate))
            );
        }

        return builder.build();
    }
}