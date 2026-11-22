package net.felix.utilities.Town.schmied;

/**
 * Trennt die frühere Standardtaste F7 von den Schmied-Rahmen, ohne eine selbst gewählte Taste anzufassen.
 */
public final class SchmiedFramesKeyMigration {

    public enum Action {
        UNBIND,
        MARK,
        NONE
    }

    private SchmiedFramesKeyMigration() {
    }

    public static Action decide(boolean alreadyReleased, boolean boundToF7) {
        if (alreadyReleased) {
            return Action.NONE;
        }
        return boundToF7 ? Action.UNBIND : Action.MARK;
    }
}
