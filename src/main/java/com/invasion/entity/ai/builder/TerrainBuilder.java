package com.invasion.entity.ai.builder;

import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.invasion.entity.NexusEntity;
import com.invasion.entity.pathfinding.ClimberUtil;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.nexus.ai.scaffold.Scaffold;
import com.invasion.util.math.PosUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

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
        BlockPos.Mutable mutable = pos.mutableCopy();

        World world = mob.asEntity().getWorld();
        BlockState block = world.getBlockState(mutable.set(pos).move(offset).move(Direction.DOWN));
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();

        // NEU: Ladder-State mit richtigem Facing
        BlockState ladderState = Blocks.LADDER.getDefaultState()
                .with(LadderBlock.FACING, offset.getOpposite());


        // Unterste Ebene direkt über Boden / Einstieg
        if (height == 1) {
            // Block vor der Leiter (Boden) mit Planks auffüllen
            if (!block.isFullCube(world, mutable)) {
                builder.add(new ModifyBlockEntry(
                        mutable.toImmutable(),
                        Blocks.OAK_PLANKS.getDefaultState(),
                        (int) (PLANKS_COST / buildRate)
                ));
            }

            // Leiter unterhalb der aktuellen Position setzen,
            // aber nur wenn da überhaupt eine Leiter dran halten kann
            mutable.set(pos).move(Direction.DOWN);
            if (world.isAir(mutable)
                    && ClimberUtil.canPositionSupportLadder(world, mutable, offset)) {
                builder.add(new ModifyBlockEntry(
                        mutable.toImmutable(),
                        ladderState,
                        (int) (LADDER_COST / buildRate)
                ));
            }
        }

        // Block vor der Leiter (Scaffold-Wand) auf dieser Höhe schließen
        mutable.set(pos).move(offset);
        if (!world.getBlockState(mutable).isFullCube(world, mutable)) {
            builder.add(new ModifyBlockEntry(
                    mutable.toImmutable(),
                    Blocks.OAK_PLANKS.getDefaultState(),
                    (int) (PLANKS_COST / buildRate)
            ));
        }

        // Leiter auf der aktuellen Scaffold-Position
        mutable.set(pos);
        if (!world.getBlockState(mutable).isOf(Blocks.LADDER)
                && ClimberUtil.canPositionSupportLadder(world, mutable, offset)) {
            builder.add(new ModifyBlockEntry(
                    mutable.toImmutable(),
                    ladderState,
                    (int) (LADDER_COST / buildRate)
            ));
        }

        // Plattform-Layer: Ring aus Planks um die Leiter herum
        if (scaffold.isPlatformLayer(height)) {
            for (Vec3i i : PosUtils.OFFSET_RING) {
                if (!i.equals(offset.getVector())) {
                    mutable.set(pos).move(i);
                    if (!world.getBlockState(mutable).isFullCube(world, mutable)) {
                        builder.add(new ModifyBlockEntry(
                                mutable.toImmutable(),
                                Blocks.OAK_PLANKS.getDefaultState(),
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
        World world = mob.asEntity().getWorld();

        if (!orientation.getAxis().isHorizontal()) {
            orientation = mob.asEntity().getHorizontalFacing();
        }

        BlockState ladderState = Blocks.LADDER.getDefaultState()
                .with(LadderBlock.FACING, orientation.getOpposite());

        BlockPos.Mutable mutable = basePos.mutableCopy();

        // Wenn layersToBuild zu klein ist (oder 0), bau wenigstens 6 hoch,
        // damit man den Effekt deutlich sieht.
        int height = Math.max(layersToBuild, 6);

        for (int i = 0; i < height; i++) {
            // Position der Leiter
            mutable.set(basePos).move(Direction.UP, i);
            BlockPos ladderPos = mutable.toImmutable();

            // Support dahinter
            BlockPos supportPos = ladderPos.offset(orientation);

            if (!world.getBlockState(supportPos).isFullCube(world, mutable.set(supportPos))) {
                builder.add(new ModifyBlockEntry(
                        supportPos,
                        Blocks.OAK_PLANKS.getDefaultState(),
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
        World world = mob.asEntity().getWorld();

        // Orientierung sicherstellen
        if (!orientation.getAxis().isHorizontal()) {
            orientation = mob.asEntity().getHorizontalFacing();
        }

        BlockState ladderState = Blocks.LADDER.getDefaultState()
                .with(LadderBlock.FACING, orientation.getOpposite());

        BlockPos.Mutable mutable = basePos.mutableCopy();

        // Tiefe begrenzen
        int depthClamped = Math.max(1, Math.min(depth, 32));

        for (int i = 1; i <= depthClamped; i++) {
            // Leiter-Position nach unten
            mutable.set(basePos).move(Direction.DOWN, i);
            BlockPos ladderPos = mutable.toImmutable();

            // Support-Block hinter der Leiter
            BlockPos supportPos = ladderPos.offset(orientation);
            if (!world.getBlockState(supportPos).isFullCube(world, mutable.set(supportPos))) {
                builder.add(new ModifyBlockEntry(
                        supportPos,
                        Blocks.OAK_PLANKS.getDefaultState(),
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
    private boolean canModifyBlock(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // Luft ignorieren
        if (state.isAir()) {
            return false;
        }

        // Unzerstörbare Blöcke (Bedrock etc.) nicht anfassen
        if (state.getHardness(world, pos) < 0.0F) {
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
        World world = mob.asEntity().getWorld();

        // Sicherheitslimit
        int depthClamped = Math.max(1, Math.min(depth, 32));

        // *** WICHTIGER UNTERSCHIED ***
        // Wir nehmen NICHT basePos, sondern IMMER die echte Mob-Position
        BlockPos entityPos = mob.asEntity().getBlockPos();
        BlockPos.Mutable mutable = entityPos.mutableCopy();

        for (int i = 1; i <= depthClamped; i++) {
            // Oberer Block dieses "Segments"
            mutable.set(entityPos).move(Direction.DOWN, i);
            BlockPos pos0 = mutable.toImmutable();

            // Block direkt darunter -> 2-Blöcke-Höhe
            mutable.move(Direction.DOWN);
            BlockPos pos1 = mutable.toImmutable();

            if (canModifyBlock(world, pos0)) {
                builder.add(new ModifyBlockEntry(
                        pos0,
                        Blocks.AIR.getDefaultState(),
                        (int) (DIG_COST / buildRate)
                ));
            }

            if (canModifyBlock(world, pos1)) {
                builder.add(new ModifyBlockEntry(
                        pos1,
                        Blocks.AIR.getDefaultState(),
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
        World world = mob.asEntity().getWorld();

        if (!dir.getAxis().isHorizontal()) {
            dir = mob.asEntity().getHorizontalFacing();
        }

        int clamped = Math.max(1, Math.min(steps, 16));
        BlockPos entityPos = mob.asEntity().getBlockPos();
        BlockPos.Mutable mut = entityPos.mutableCopy();

        for (int i = 1; i <= clamped; i++) {
            // „Stufe“, auf die er treten soll:
            mut.set(entityPos).move(dir, i).move(Direction.UP, i - 1);
            BlockPos stepPos = mut.toImmutable();

            // Kopf-Freiraum: Block über der Stufe + noch einer drüber
            BlockPos head1 = stepPos.up();
            BlockPos head2 = stepPos.up(2);

            // Stufe platzieren, wenn leer
            if (world.getBlockState(stepPos).isAir()) {
                builder.add(new ModifyBlockEntry(
                        stepPos,
                        Blocks.COBBLESTONE.getDefaultState(),  // oder Slab/Stair
                        (int) (COBBLE_COST / buildRate)
                ));
            } else if (canModifyBlock(world, stepPos)) {
                // Wenn da schon irgendwas im Weg ist: weg damit und dann unsere Stufe drauf
                builder.add(new ModifyBlockEntry(
                        stepPos,
                        Blocks.COBBLESTONE.getDefaultState(),
                        (int) (COBBLE_COST / buildRate)
                ));
            }

            // Kopf freimachen
            if (canModifyBlock(world, head1)) {
                builder.add(new ModifyBlockEntry(
                        head1,
                        Blocks.AIR.getDefaultState(),
                        (int) (DIG_COST / buildRate)
                ));
            }
            if (canModifyBlock(world, head2)) {
                builder.add(new ModifyBlockEntry(
                        head2,
                        Blocks.AIR.getDefaultState(),
                        (int) (DIG_COST / buildRate)
                ));
            }
        }

        return builder.build();
    }



    @Override
    public Stream<ModifyBlockEntry> askBuildLadder(BlockPos pos, Direction orientation) {
        Stream.Builder<ModifyBlockEntry> builder = Stream.builder();
        World world = mob.asEntity().getWorld();
        BlockPos.Mutable mutable = pos.mutableCopy();

        // Leiter nach vorn ausgerichtet
        BlockState ladderState = Blocks.LADDER.getDefaultState()
                .with(LadderBlock.FACING, orientation.getOpposite());

        // Wir bauen pauschal 4 Blöcke hoch (kannst du später erhöhen)
        int height = 4;

        for (int i = 0; i < height; i++) {
            // Position der Leiter
            mutable.set(pos).move(Direction.UP, i);
            BlockPos ladderPos = mutable.toImmutable();

            // Block HINTER der Leiter (Support)
            BlockPos supportPos = ladderPos.offset(orientation);

            // Support immer aus Planks setzen, wenn nicht voll
            if (!world.getBlockState(supportPos).isFullCube(world, mutable.set(supportPos))) {
                builder.add(new ModifyBlockEntry(
                        supportPos,
                        Blocks.OAK_PLANKS.getDefaultState(),
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

        BlockPos.Mutable mutable = pos.mutableCopy();
        BlockPos posBelow = pos.down();
        World world = mob.asEntity().getWorld();

        boolean isFluid = world.getBlockState(pos).isLiquid();
        boolean isAirBelow = world.isAir(posBelow);

        if (isAirBelow || isFluid) {
            if (isFluid) {
                posBelow = pos;
            }
            boolean needsSupport = IMLandPathNodeMaker.avoidsBlock(mob.asEntity(), mutable.set(pos).move(Direction.DOWN, 2))
                                || IMLandPathNodeMaker.avoidsBlock(mob.asEntity(), mutable.set(pos).move(Direction.DOWN, 3));
            builder.add(new ModifyBlockEntry(posBelow,
                    (needsSupport ? Blocks.COBBLESTONE : Blocks.OAK_PLANKS).getDefaultState(),
                    (int) ((needsSupport ? COBBLE_COST : PLANKS_COST) / buildRate))
            );
        }

        return builder.build();
    }
}