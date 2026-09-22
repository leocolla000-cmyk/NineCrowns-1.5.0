package it.lia.ninecrowns;

import java.util.*;

public final class Roster {
    public static final int SIZE = 7;
    private final LinkedHashMap<UUID, String> players = new LinkedHashMap<>();

    public boolean register(UUID id, String name) {
        if (players.containsKey(id)) {
            players.put(id, name);
            return true;
        }
        if (players.size() >= SIZE) return false;
        players.put(id, name);
        return true;
    }

    public boolean contains(UUID id) {
        return players.containsKey(id);
    }

    public Map<UUID, String> players() {
        return Collections.unmodifiableMap(players);
    }

    /**
     * A valid OP craft for seven players must contain exactly the six OTHER
     * registered participants. The crafter's own head is therefore rejected,
     * and duplicates cannot replace a missing player's head.
     */
    public boolean validHeads(UUID crafter, List<UUID> heads) {
        if (players.size() != SIZE || !players.containsKey(crafter) || heads.size() != SIZE - 1) {
            return false;
        }

        Set<UUID> unique = new HashSet<>(heads);
        if (unique.size() != SIZE - 1 || unique.contains(crafter)) {
            return false;
        }

        Set<UUID> expected = new HashSet<>(players.keySet());
        expected.remove(crafter);
        return unique.equals(expected);
    }
}
