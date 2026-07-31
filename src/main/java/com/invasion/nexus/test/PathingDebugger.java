package com.invasion.nexus.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.PathfindingDebugPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Target;
import org.apache.commons.lang3.stream.IntStreams;
import org.jetbrains.annotations.Nullable;

import com.invasion.Debug;

public class PathingDebugger {

    public static void sendPathToClients(Entity sender, @Nullable Path path, float scale) {
        if (Debug.DEBUG_PATHFINDING) {
            sender.getServer().getPlayerList().broadcastAll(new ClientboundCustomPayloadPacket(new PathfindingDebugPayload(sender.getId(), createDebuggablePath(path), scale)));
        }
    }

    private static Path createDebuggablePath(Path path) {
        return new Path(List.of(), BlockPos.ZERO, false) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public void writeToStream(FriendlyByteBuf buf) {
                buf.writeBoolean(path.canReach());
                buf.writeInt(path.getNextNodeIndex());
                buf.writeBlockPos(path.getTarget());

                Set<Node> open = new HashSet<>();
                Set<Node> closed = new HashSet<>();
                Set<Target> targets = new HashSet<>();

                buf.writeCollection(IntStreams.range(path.getNodeCount()).mapToObj(path::getNode).peek(node -> {
                    ((Set)(node instanceof Target ? targets : node.closed ? closed : open)).add(node instanceof Target t ? t : node);
                }).toList(), (bufx, node) -> node.writeToStream(bufx));


                new Path.DebugData(open.toArray(Node[]::new), closed.toArray(Node[]::new), targets).write(buf);
            }
        };
    }
}
