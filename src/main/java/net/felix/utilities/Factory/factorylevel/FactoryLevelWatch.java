package net.felix.utilities.Factory.factorylevel;

import java.util.Objects;

/**
 * Merkt sich die Fabrikerkennung. Die eigentliche Prüfung läuft nur,
 * wenn die Welt oder die Dimensionskennung sich geändert hat.
 */
public final class FactoryLevelWatch {
    private Object world;
    private String dimensionId;
    private boolean inFactory;

    public synchronized void reset() {
        world = null;
        dimensionId = null;
        inFactory = false;
    }

    public synchronized boolean observe(Object world, String dimensionId, String playerName) {
        if (this.world == world && Objects.equals(this.dimensionId, dimensionId)) {
            return false;
        }
        this.world = world;
        this.dimensionId = dimensionId;
        inFactory = FactoryLevelPresence.isFactory(dimensionId, playerName);
        return true;
    }

    public synchronized boolean inFactory() {
        return inFactory;
    }
}
