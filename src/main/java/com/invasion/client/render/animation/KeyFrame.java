package com.invasion.client.render.animation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record KeyFrame(
        float time,
        Vector3fc rotation,
        Vector3fc pivot,
        InterpType interpType,
        boolean hasPos) {
    static final Vector3fc ZERO_VECTOR = new Vector3f(0, 0, 0);

	public KeyFrame(float time, float rotX, float rotY, float rotZ, InterpType interpType) {
		this(time, new Vector3f(rotX, rotY, rotZ).mul(Mth.DEG_TO_RAD), ZERO_VECTOR, interpType, false);
	}

	public KeyFrame(float time, float rotX, float rotY, float rotZ, float posX, float posY, float posZ, InterpType interpType) {
	    this(time, new Vector3f(rotX, rotY, rotZ).mul(Mth.DEG_TO_RAD), new Vector3f(posX, posY, posZ), interpType, true);
	}

	@Deprecated
	public static List<KeyFrame> toRadians(List<KeyFrame> keyFrames) {
	    return keyFrames;
	}

	public static List<KeyFrame> mirrorFramesX(List<KeyFrame> keyFrames) {
		return keyFrames.stream().map(keyFrame -> new KeyFrame(keyFrame.time(),
		        keyFrame.rotation().mul(1, -1, -1, new Vector3f()),
		        keyFrame.pivot().mul(-1, 1, 1, new Vector3f()),
		        keyFrame.interpType(),
		        keyFrame.hasPos
        )).toList();
	}

   public static List<KeyFrame> mirrorFramesY(List<KeyFrame> keyFrames) {
        return keyFrames.stream().map(keyFrame -> new KeyFrame(keyFrame.time(),
                keyFrame.rotation().mul(-1, 1, -1, new Vector3f()),
                keyFrame.pivot().mul(1, -1, 1, new Vector3f()),
                keyFrame.interpType(),
                keyFrame.hasPos
        )).toList();
    }

   public static List<KeyFrame> mirrorFramesZ(List<KeyFrame> keyFrames) {
       return keyFrames.stream().map(keyFrame -> new KeyFrame(keyFrame.time(),
               keyFrame.rotation().mul(-1, -1, 1, new Vector3f()),
               keyFrame.pivot().mul(1, 1, -1, new Vector3f()),
               keyFrame.interpType(),
               keyFrame.hasPos
       )).toList();
   }

   public static Vector3fc lerp(float delta, Vector3fc from, Vector3fc to, Vector3f output) {
       return to.sub(from, new Vector3f()).mul(delta).add(to);
   }

	public static List<KeyFrame> offsetFramesCircular(List<KeyFrame> keyFrames, float start, float end, float offset) {
       if (keyFrames.isEmpty()) {
            return keyFrames;
        }
	    keyFrames = new ArrayList<>(keyFrames);
		float diff = end - start;
		offset %= diff;
		float k1 = end - offset;
		List<KeyFrame> copy = new ArrayList<>(keyFrames);
		keyFrames.clear();
		KeyFrame currFrame = null;
		ListIterator<KeyFrame> iter = copy.listIterator();

		while (iter.hasNext()) {
			currFrame = iter.next();
			if (currFrame.time() >= start)
				break;
			keyFrames.add(currFrame);
		}

		List<KeyFrame> buffer = new ArrayList<>();
		buffer.add(currFrame);
		while (iter.hasNext()) {
			currFrame = iter.next();
			if (currFrame.time() >= k1) {
				break;
			}
			buffer.add(currFrame);
		}
		KeyFrame fencepostStart;
		if (!Mth.equal(currFrame.time(), k1)) {
			iter.previous();
			KeyFrame prev = iter.previous();

			float dt = k1 - prev.time();
			float dtFrame = currFrame.time() - prev.time();
			float r = dt / dtFrame;

			Vector3fc rot = lerp(r, prev.rotation(), currFrame.rotation(), new Vector3f());
			fencepostStart = new KeyFrame(start, rot, ZERO_VECTOR, InterpType.LINEAR, false);
		} else {
			fencepostStart = currFrame;
		}

		keyFrames.add(fencepostStart);

		while (iter.hasNext()) {
			currFrame = iter.next();
			if (currFrame.time() <= end) {
				float t = currFrame.time() + offset - diff;
				keyFrames.add(new KeyFrame(t, currFrame.rotation(), currFrame.pivot(), InterpType.LINEAR, currFrame.hasPos));
			} else {
				//UnstoppableN testcode, this seemed to fix some issues, not sure why
				//iter.previous();
			}
		}

		Iterator<KeyFrame> iter2 = buffer.iterator();
		while (iter2.hasNext()) {
			currFrame = iter2.next();
			float t = currFrame.time() + offset;
			keyFrames.add(new KeyFrame(t, currFrame.rotation(), currFrame.pivot(), InterpType.LINEAR, currFrame.hasPos));
		}

		keyFrames.add(new KeyFrame(end, fencepostStart.rotation(), ZERO_VECTOR, InterpType.LINEAR, false));

		while (iter.hasNext()) {
			keyFrames.add(iter.next());
		}

		return keyFrames;
	}
}