package com.invasion.item;

import com.invasion.entity.TrapEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.invasion.entity.InvEntities;

public class TrapItem extends Item {

    private final TrapEntity.Type trapType;

    public TrapItem(Properties settings, TrapEntity.Type trapType) {
        super(settings.stacksTo(64).durability(0));
        this.trapType = trapType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() == Direction.UP) {
            Level world = context.getLevel();
            Vec3 pos = com.invasion.util.math.PosUtils.bottomCenter(context.getClickedPos().relative(context.getClickedFace()));
            TrapEntity trap = new TrapEntity(InvEntities.TRAP, world, pos.x(), pos.y(), pos.z(), trapType);

            if (trap.isValidPlacement()
                    && world.getEntitiesOfClass(TrapEntity.class, trap.getBoundingBox(),
                    EntitySelector.NO_CREATIVE_OR_SPECTATOR).isEmpty()) {
                if (!world.isClientSide()) {
                    world.addFreshEntity(trap);
                    context.getItemInHand().consume(1, context.getPlayer());
                }

                return InteractionResult.SUCCESS_SERVER;
            }
        }

        return InteractionResult.FAIL;
    }
}
