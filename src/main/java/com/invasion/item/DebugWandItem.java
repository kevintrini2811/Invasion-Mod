package com.invasion.item;

import org.jetbrains.annotations.Nullable;

import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import com.invasion.entity.VultureEntity;
import com.invasion.entity.IMCreeperEntity;
import com.invasion.entity.PigmanEngineerEntity;
import com.invasion.entity.IMSkeletonEntity;
import com.invasion.entity.NexusSpiderEntity;
import com.invasion.entity.ThrowerEntity;
import com.invasion.entity.EntityIMZombie;
import com.invasion.entity.InvEntities;
import com.invasion.nexus.NexusAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

class DebugWandItem extends Item {
    @Nullable
    private NexusAccess nexus;

    public DebugWandItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        if (!(context.getLevel() instanceof ServerLevel world)) {
            return InteractionResult.PASS;
        }

        BlockState state = world.getBlockState(context.getClickedPos());
        if (state.is(InvBlocks.NEXUS_CORE)) {
            this.nexus = ((NexusBlockEntity) world.getBlockEntity(context.getClickedPos())).getNexus();
            return InteractionResult.SUCCESS;
        }

        if (nexus != null && nexus.getWorld() != world) {
            nexus = null;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        VultureEntity bird = InvEntities.VULTURE.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        bird.setNexus(nexus);
        bird.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

        Zombie zombie2 = new Zombie(world);
        zombie2.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

        EntityTypes.WOLF.create(world, w -> {}, pos, EntitySpawnReason.COMMAND, true, false);

        Entity entity1 = InvEntities.PIGMAN_ENGINEER.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        entity1.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

        EntityIMZombie zombie = InvEntities.ZOMBIE.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        zombie.setNexus(nexus);
        zombie.setFlavour(0);
        zombie.setTier(1);

        zombie.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

        if (this.nexus != null) {
            PigmanEngineerEntity entity = InvEntities.PIGMAN_ENGINEER.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            entity.setNexus(nexus);
            entity.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

            zombie = new EntityIMZombie(InvEntities.ZOMBIE, world);
            zombie.setNexus(nexus);
            zombie.setFlavour(0);
            zombie.setTier(2);
            zombie.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

            ThrowerEntity thrower = InvEntities.THROWER.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            thrower.setNexus(nexus);
            thrower.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

            IMCreeperEntity creep = InvEntities.CREEPER.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            creep.setNexus(nexus);
            creep.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

            NexusSpiderEntity spider = InvEntities.JUMPING_SPIDER.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            spider.setNexus(nexus);

            spider.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

            IMSkeletonEntity skeleton = InvEntities.SKELETON.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            skeleton.setNexus(nexus);
            skeleton.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));
        }

        NexusSpiderEntity entity = InvEntities.QUEEN_SPIDER.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        entity.setNexus(nexus);

        entity.setPos(com.invasion.util.math.PosUtils.bottomCenter(pos));

        IMCreeperEntity creep = InvEntities.CREEPER.create(world, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        creep.setNexus(nexus);
        creep.setPos(150.5D, 64.0D, 271.5D);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Wolf wolf && attacker instanceof Player player) {
            wolf.tame(player);
        }
    }
}
