package com.invasion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.pathfinder.Node;

@Mixin(Node.class)
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

    @Inject(method = "cloneAndMove", at = @At("RETURN"))
    private void invasion_after_copyWithNewPosition(int x, int y, int z, CallbackInfoReturnable<Node> info) {
        ((ActionablePathNode)info.getReturnValue()).setAction(action);
    }

    @Inject(method = "writeToStream", at = @At("RETURN"))
    private void invasion_after_write(FriendlyByteBuf buf, CallbackInfo info) {
        buf.writeEnum(action);
    }

    @Inject(method = "readContents", at = @At("RETURN"))
    private static void invasion_after_readFromBuf(FriendlyByteBuf buf, Node target, CallbackInfo info) {
        ((ActionablePathNode)target).setAction(buf.readEnum(PathAction.class));
    }
    /**
     * @reason Added toString output for debugging path node actions.
     * @author Cedric
     */
    @Override
    @Overwrite
    public String toString() {
        return "Node{x=" + ((Node)(Object)this).x + ", y=" + ((Node)(Object)this).y + ", z=" + ((Node)(Object)this).z + ", action=" + action + "}";
    }
}
