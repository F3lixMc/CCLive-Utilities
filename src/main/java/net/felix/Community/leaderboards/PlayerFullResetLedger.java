package net.felix.Community.leaderboards;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Aufträge des Spieler-Fullresets. Die Seasonphase steht im Auftrag und wird später nicht umgeschrieben.
 */
public final class PlayerFullResetLedger {
    public record Order(
            String id,
            String account,
            int seasonId,
            boolean preseason,
            boolean serverApplied,
            boolean localCleared
    ) {
    }

    private final List<Order> orders = new ArrayList<>();
    private boolean screenVisible;

    /** Einmal pro Erscheinen, auch wenn der Screen viele Ticks offen bleibt. */
    public synchronized boolean opening(boolean visible) {
        if (!visible) {
            screenVisible = false;
            return false;
        }
        if (screenVisible) {
            return false;
        }
        screenVisible = true;
        return true;
    }

    public synchronized void reopen() {
        screenVisible = false;
    }

    /** Ohne Konto oder Seasonphase entsteht kein Serverauftrag. */
    public synchronized Order stage(String id, String account, Integer seasonId, boolean preseason) {
        if (id == null || id.isBlank() || account == null || account.isBlank() || seasonId == null || seasonId < 0) {
            return null;
        }
        Order order = new Order(id, account, seasonId, preseason, false, false);
        orders.add(order);
        return order;
    }

    public synchronized void drop(String id) {
        orders.removeIf(order -> order.id().equals(id));
    }

    public synchronized void copyFrom(PlayerFullResetLedger other) {
        orders.clear();
        if (other != null) {
            orders.addAll(other.orders);
        }
    }

    public synchronized void markLocalCleared(String id) {
        replace(id, true, false);
    }

    public synchronized boolean markServerApplied(String id) {
        Order current = find(id);
        if (current == null || current.serverApplied()) {
            return false;
        }
        replace(id, current.localCleared(), true);
        return true;
    }

    public synchronized boolean blocks(String account, int seasonId, boolean preseason) {
        for (Order order : orders) {
            if (matches(order, account, seasonId, preseason) && !order.serverApplied()) {
                return true;
            }
        }
        return false;
    }

    /** Die zuletzt bestätigte Id dieser Phase. Eine ältere Wiederholung ersetzt sie nicht. */
    public synchronized String uploadId(String account, int seasonId, boolean preseason) {
        String latest = null;
        for (Order order : orders) {
            if (matches(order, account, seasonId, preseason) && order.serverApplied()) {
                latest = order.id();
            }
        }
        return latest;
    }

    public synchronized List<Order> pendingLocal() {
        List<Order> pending = new ArrayList<>();
        for (Order order : orders) {
            if (!order.localCleared()) {
                pending.add(order);
            }
        }
        return pending;
    }

    public synchronized List<Order> pendingServer() {
        List<Order> pending = new ArrayList<>();
        for (Order order : orders) {
            if (order.localCleared() && !order.serverApplied()) {
                pending.add(order);
            }
        }
        return pending;
    }

    public synchronized JsonObject toJson() {
        JsonArray array = new JsonArray();
        for (Order order : orders) {
            JsonObject row = new JsonObject();
            row.addProperty("id", order.id());
            row.addProperty("account", order.account());
            row.addProperty("seasonId", order.seasonId());
            row.addProperty("preseason", order.preseason());
            row.addProperty("serverApplied", order.serverApplied());
            row.addProperty("localCleared", order.localCleared());
            array.add(row);
        }
        JsonObject root = new JsonObject();
        root.add("orders", array);
        return root;
    }

    public static PlayerFullResetLedger fromJson(JsonObject root) {
        PlayerFullResetLedger ledger = new PlayerFullResetLedger();
        if (root == null || !root.has("orders") || !root.get("orders").isJsonArray()) {
            return ledger;
        }
        for (var element : root.getAsJsonArray("orders")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            if (!row.has("id") || !row.has("account") || !row.has("seasonId")) {
                continue;
            }
            int seasonId = row.get("seasonId").getAsInt();
            if (seasonId < 0) {
                continue;
            }
            ledger.orders.add(new Order(
                    row.get("id").getAsString(),
                    row.get("account").getAsString(),
                    seasonId,
                    row.has("preseason") && row.get("preseason").getAsBoolean(),
                    row.has("serverApplied") && row.get("serverApplied").getAsBoolean(),
                    row.has("localCleared") && row.get("localCleared").getAsBoolean()
            ));
        }
        return ledger;
    }

    private void replace(String id, boolean keepLocal, boolean serverApplied) {
        for (int index = 0; index < orders.size(); index++) {
            Order order = orders.get(index);
            if (!order.id().equals(id)) {
                continue;
            }
            boolean localCleared = order.localCleared() || keepLocal;
            orders.set(index, new Order(order.id(), order.account(), order.seasonId(), order.preseason(), serverApplied, localCleared));
            return;
        }
    }

    private Order find(String id) {
        for (Order order : orders) {
            if (order.id().equals(id)) {
                return order;
            }
        }
        return null;
    }

    private static boolean matches(Order order, String account, int seasonId, boolean preseason) {
        return order.account().equals(account) && order.seasonId() == seasonId && order.preseason() == preseason;
    }
}
