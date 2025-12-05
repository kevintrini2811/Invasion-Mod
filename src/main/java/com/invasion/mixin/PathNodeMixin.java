package com.invasion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;

import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.network.PacketByteBuf;

@Mixin(PathNode.class)
abstract class PathNodeMixin implements ActionablePathNode {
    private PathAction action = PathAction.NONE;

    @Override
    public PathAction getAction() {
        return action;
    }

    @Override
    public void setAction(PathAction action) {
        this.action = action;
    }

    @Inject(method = "copyWithNewPosition", at = @At("RETURN"))
    private void invasion_after_copyWithNewPosition(int x, int y, int z, CallbackInfoReturnable<PathNode> info) {
        ((ActionablePathNode)info.getReturnValue()).setAction(action);
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void invasion_after_write(PacketByteBuf buf, CallbackInfo info) {
        buf.writeEnumConstant(action);
    }

    @Inject(method = "readFromBuf", at = @At("RETURN"))
    private static void invasion_after_readFromBuf(PacketByteBuf buf, PathNode target, CallbackInfo info) {
        ((ActionablePathNode)target).setAction(buf.readEnumConstant(PathAction.class));
    }
    /**
     * @reason Added toString output for debugging path node actions.
     * @author Cedric
     */
    @Override
    @Overwrite
    public String toString() {
        return "Node{x=" + ((PathNode)(Object)this).x + ", y=" + ((PathNode)(Object)this).y + ", z=" + ((PathNode)(Object)this).z + ", action=" + action + "}";
    }
}
