package com.invasion.compat;

/** Native jump steering, independent of a mod's move-control implementation. */
public interface NexusJumpControl {
    void invasion$setDirection(float direction, boolean aggressive);

    void invasion$setWantedMovement(double speed);
}
