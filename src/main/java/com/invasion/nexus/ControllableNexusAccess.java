package com.invasion.nexus;

public interface ControllableNexusAccess extends NexusAccess {

    boolean start(int wave);

    void stop(boolean killEnemies);

    boolean setSpawnRadius(int radius);

    /**
     * Schaltet den Pause-Zustand um.
     * @return true, wenn danach PAUSIERT ist; false, wenn danach wieder läuft.
     */
    default boolean togglePause() {
        return false;
    }

    /**
     * @return true, wenn der Nexus gerade im Pausenmodus ist.
     */
    default boolean isPaused() {
        return false;
    }
}
