package it.lia.ninecrowns;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

public final class WorldData {
    public final Roster roster = new Roster();

    private static final class LightningState {
        int charges;
        long nextRecharge;

        LightningState(int charges, long nextRecharge) {
            this.charges = charges;
            this.nextRecharge = nextRecharge;
        }
    }

    private final Map<UUID, LightningState> lightning = new HashMap<>();
    private final Path path;

    public WorldData(Path path) {
        this.path = path;
        if (!Files.exists(path)) return;

        try {
            var root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            // Migration from pre-1.5.0 rosters: old saves do not contain the
            // seven-player rosterSize marker, so registration starts fresh once.
            // After the first 1.5.0 save, partial/full seven-player rosters persist normally.
            if (root.has("players")
                && root.has("rosterSize")
                && root.get("rosterSize").getAsInt() == Roster.SIZE) {
                var players = root.getAsJsonObject("players");
                for (var e : players.entrySet()) {
                    roster.register(UUID.fromString(e.getKey()), e.getValue().getAsString());
                }
            }

            // New charge format. Old primitive cooldown values from pre-charge
            // versions are intentionally ignored, giving the new system a clean start.
            if (root.has("lightning")) {
                for (var e : root.getAsJsonObject("lightning").entrySet()) {
                    if (!e.getValue().isJsonObject()) continue;
                    var obj = e.getValue().getAsJsonObject();
                    int charges = obj.has("charges") ? obj.get("charges").getAsInt() : 2;
                    long next = obj.has("nextRecharge") ? obj.get("nextRecharge").getAsLong() : 0L;
                    lightning.put(UUID.fromString(e.getKey()), new LightningState(charges, next));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Nine Crowns world data error", e);
        }
    }

    private LightningState state(UUID id, int maxCharges) {
        return lightning.computeIfAbsent(id, ignored -> new LightningState(maxCharges, 0L));
    }

    private static void normalize(LightningState state, long now, int maxCharges, long rechargeTicks) {
        state.charges = Math.max(0, Math.min(maxCharges, state.charges));

        if (state.charges >= maxCharges) {
            state.charges = maxCharges;
            state.nextRecharge = 0L;
            return;
        }

        if (state.nextRecharge <= 0L) {
            state.nextRecharge = now + rechargeTicks;
            return;
        }

        while (state.charges < maxCharges && now >= state.nextRecharge) {
            state.charges++;
            if (state.charges < maxCharges) {
                state.nextRecharge += rechargeTicks;
            } else {
                state.nextRecharge = 0L;
            }
        }
    }

    public int lightningCharges(UUID id, long now, int maxCharges, long rechargeTicks) {
        LightningState state = state(id, maxCharges);
        normalize(state, now, maxCharges, rechargeTicks);
        return state.charges;
    }

    public long lightningRemaining(UUID id, long now, int maxCharges, long rechargeTicks) {
        LightningState state = state(id, maxCharges);
        normalize(state, now, maxCharges, rechargeTicks);
        if (state.charges >= maxCharges || state.nextRecharge <= 0L) return 0L;
        return Math.max(0L, state.nextRecharge - now);
    }

    public boolean consumeLightning(UUID id, long now, int maxCharges, long rechargeTicks) {
        LightningState state = state(id, maxCharges);
        normalize(state, now, maxCharges, rechargeTicks);
        if (state.charges <= 0) return false;

        state.charges--;
        if (state.nextRecharge <= 0L) {
            state.nextRecharge = now + rechargeTicks;
        }
        save();
        return true;
    }

    public void save() {
        try {
            var root = new JsonObject();
            var ps = new JsonObject();
            var cds = new JsonObject();

            roster.players().forEach((id, name) -> ps.addProperty(id.toString(), name));
            lightning.forEach((id, state) -> {
                var obj = new JsonObject();
                obj.addProperty("charges", state.charges);
                obj.addProperty("nextRecharge", state.nextRecharge);
                cds.add(id.toString(), obj);
            });

            root.addProperty("rosterSize", Roster.SIZE);
            root.add("players", ps);
            root.add("lightning", cds);
            Files.createDirectories(path.getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception e) {
            throw new IllegalStateException("Nine Crowns save error", e);
        }
    }
}
