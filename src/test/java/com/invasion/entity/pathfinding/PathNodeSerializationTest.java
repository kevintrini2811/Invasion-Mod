package com.invasion.entity.pathfinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.world.level.pathfinder.Node;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PathNodeSerializationTest {
    @ParameterizedTest
    @EnumSource(PathAction.class)
    void debugCodecAndCloningPreservePathAction(PathAction action) {
        Node original = new Node(12, 64, -7);
        assertInstanceOf(ActionablePathNode.class, original).setAction(action);
        ByteBuf buffer = Unpooled.buffer();
        try {
            Node.DEBUG_STREAM_CODEC.encode(buffer, original);
            Node decoded = Node.DEBUG_STREAM_CODEC.decode(buffer);
            assertEquals(original.asBlockPos(), decoded.asBlockPos());
            assertEquals(action, ActionablePathNode.getAction(decoded));
            assertEquals(action, ActionablePathNode.getAction(decoded.cloneAndMove(2, 3, 4)));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
