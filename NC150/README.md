# Nine Crowns 1.5.0 — Minecraft 26.1 Fabric

Private seven-player PvP mod.

## Seven-player roster and heads
- The first seven distinct players registered in the world become the fixed roster.
- Every registered player always drops their own named player head on death.
- OP recipes require exactly the six different heads of the OTHER six registered players.
- The crafter's own head is rejected.
- Duplicate heads cannot replace a missing player's head.
- When upgrading from an old saved roster larger than seven players, the roster is re-registered for the new seven-player setup.

## OP crafting layout
All three recipes use this shape:

    H H H
    H X H
    M H M

`H` = six different opponent heads.

- OP Sword: `X` = Netherite Sword, `M` = Diamond.
- OP Spear: `X` = Netherite Spear, `M` = Diamond.
- Emperor Crown: `X` = Empty Crown, `M` = Netherite Ingot.

## OP Spear
- Sharpness V.
- Lunge III.
- Unbreakable.
- Two lightning charges maximum.
- One charge regenerates every 30 seconds.
- Both charges can be fired consecutively when available.
- Each lightning strike deals 5 damage points (2.5 hearts), is visual-only/no fire, and keeps the existing aimed-target behavior.

## Other special items
- OP Sword keeps its existing damage/effects and visuals.
- Emperor Crown keeps Protection X, Speed II, Strength II, Fire Resistance I, Unbreakable, and the 1.4.3 visual assets.
- Empty Crown recipe/effects remain unchanged and it stays Unbreakable.
- No automatic OP/granting code is included.

Build with Java 25 and Gradle 9.5.1. GitHub Actions is included in `.github/workflows/main.yml`.
