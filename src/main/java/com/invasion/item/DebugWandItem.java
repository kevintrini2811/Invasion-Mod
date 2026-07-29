package com.invasion.item;

import org.jetbrains.annotations.Nullable;

import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
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
        VultureEntity bird = InvEntities.VULTURE.create(world);
        bird.setNexus(nexus);
        bird.setPos(pos.getBottomCenter());

        Zombie zombie2 = new Zombie(world);
        zombie2.setPos(pos.getBottomCenter());

        EntityType.WOLF.create(world, w -> {}, pos, MobSpawnType.COMMAND, true, false);

        Entity entity1 = InvEntities.PIGMAN_ENGINEER.create(world);
        entity1.setPos(pos.getBottomCenter());

        EntityIMZombie zombie = InvEntities.ZOMBIE.create(world);
        zombie.setNexus(nexus);
        zombie.setFlavour(0);
        zombie.setTier(1);

        zombie.setPos(pos.getBottomCenter());

        if (this.nexus != null) {
            PigmanEngineerEntity entity = InvEntities.PIGMAN_ENGINEER.create(world);
            entity.setNexus(nexus);
            entity.setPos(pos.getBottomCenter());

            zombie = new EntityIMZombie(InvEntities.ZOMBIE, world);
            zombie.setNexus(nexus);
            zombie.setFlavour(0);
            zombie.setTier(2);
            zombie.setPos(pos.getBottomCenter());

            ThrowerEntity thrower = InvEntities.THROWER.create(world);
            thrower.setNexus(nexus);
            thrower.setPos(pos.getBottomCenter());

            IMCreeperEntity creep = InvEntities.CREEPER.create(world);
            creep.setNexus(nexus);
            creep.setPos(pos.getBottomCenter());

            NexusSpiderEntity spider = InvEntities.JUMPING_SPIDER.create(world);
            spider.setNexus(nexus);

            spider.setPos(pos.getBottomCenter());

            IMSkeletonEntity skeleton = InvEntities.SKELETON.create(world);
            skeleton.setNexus(nexus);
            skeleton.setPos(pos.getBottomCenter());
        }

        NexusSpiderEntity entity = InvEntities.QUEEN_SPIDER.create(world);
        entity.setNexus(nexus);

        entity.setPos(pos.getBottomCenter());

        IMCreeperEntity creep = InvEntities.CREEPER.create(world);
        creep.setNexus(nexus);
        creep.setPos(150.5D, 64.0D, 271.5D);

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Wolf wolf && attacker instanceof Player player) {
            wolf.tame(player);
            return true;
        }
        return false;
    }
}