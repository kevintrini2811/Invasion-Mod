package com.invasion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.mojang.datafixers.util.Function3;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
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

    @Inject(method = "createDebugStreamCodec", at = @At("RETURN"), cancellable = true)
    private static <N extends Node> void invasion$includePathAction(
            Function3<Integer, Integer, Integer, N> factory,
            CallbackInfoReturnable<StreamCodec<ByteBuf, N>> info) {
        StreamCodec<ByteBuf, N> vanilla = info.getReturnValue();
        info.setReturnValue(StreamCodec.of((buffer, node) -> {
            vanilla.encode(buffer, node);
            new FriendlyByteBuf(buffer).writeEnum(((ActionablePathNode) node).getAction());
        }, buffer -> {
            N node = vanilla.decode(buffer);
            ((ActionablePathNode) node).setAction(new FriendlyByteBuf(buffer).readEnum(PathAction.class));
            return node;
        }));
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
