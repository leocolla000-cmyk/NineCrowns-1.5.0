package it.lia.ninecrowns;

import com.google.gson.GsonBuilder;
import java.nio.file.*;

public final class Config {
    public double swordDamage = 14, jabDamage = 10;
    public double poisonChance = .20, slownessChance = .10;
    public int effectTicks = 60, jabCooldownTicks = 20;

    // OP spear lightning: two stored charges; one charge regenerates every 30 seconds.
    public int lightningMaxCharges = 2, lightningRechargeSeconds = 30;
    public double lightningRange = 24;
    public float lightningDamage = 5;

    public static Config load(Path path) {
        try {
            var gson = new GsonBuilder().setPrettyPrinting().create();
            Config c = Files.exists(path)
                ? gson.fromJson(Files.readString(path), Config.class)
                : new Config();
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(c));
            return c;
        } catch (Exception e) {
            throw new IllegalStateException("Nine Crowns config error", e);
        }
    }
}
