package it.lia.ninecrowns;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraft.commands.Commands;

public final class NineCrowns implements ModInitializer {
    public static Config config;
    public static WorldData data;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("ninecrowns", path);
    }

    @Override
    public void onInitialize() {
        config = Config.load(FabricLoader.getInstance().getConfigDir().resolve("ninecrowns.json"));
        ModItems.init();

        ServerLifecycleEvents.SERVER_STARTING.register(server ->
            data = new WorldData(server.getWorldPath(LevelResource.ROOT).resolve("data/ninecrowns.json"))
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            server.getPlayerList().getPlayers().forEach(CrownEffects::clear);
            if (data != null) data.save();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> data = null);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            if (data == null) return;
            boolean member = data.roster.register(player.getUUID(), player.getGameProfile().name());
            data.save();
            player.sendSystemMessage(Component.literal(
                member
                    ? "Nine Crowns: partecipanti registrati " + data.roster.players().size() + "/7."
                    : "Nine Crowns: elenco completo; questo account non e un partecipante."
            ));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CrownEffects.clear(handler.player));

        ServerTickEvents.END_SERVER_TICK.register(server ->
            server.getPlayerList().getPlayers().forEach(player -> {
                CrownEffects.update(player);

                // Also fixes special items obtained through Creative or /give.
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ModItems.applyRequiredProperties(player.getInventory().getItem(slot), player.registryAccess());
                }
                ModItems.applyRequiredProperties(player.getMainHandItem(), player.registryAccess());
                ModItems.applyRequiredProperties(player.getOffhandItem(), player.registryAccess());
                ModItems.applyRequiredProperties(player.getItemBySlot(EquipmentSlot.HEAD), player.registryAccess());
            })
        );

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && data != null && data.roster.contains(player.getUUID())) {
                var head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
                head.set(DataComponents.CUSTOM_NAME, Component.literal("Testa di " + player.getGameProfile().name()));
                player.spawnAtLocation(player.level(), head);
            }
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamage, damageTaken, blocked) -> {
            if (blocked || damageTaken <= 0) return;
            if (source.getDirectEntity() instanceof ServerPlayer attacker && attacker.getMainHandItem().is(ModItems.SWORD)) {
                var random = attacker.getRandom();
                if (random.nextFloat() < config.poisonChance) {
                    victim.addEffect(new MobEffectInstance(MobEffects.POISON, config.effectTicks, 0), attacker);
                }
                if (random.nextFloat() < config.slownessChance) {
                    victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, config.effectTicks, 0), attacker);
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("ninecrowns").executes(context -> {
                if (data == null) {
                    context.getSource().sendFailure(Component.literal("Nine Crowns: dati non ancora caricati."));
                    return 0;
                }
                String names = data.roster.players().isEmpty()
                    ? "nessuno"
                    : String.join(", ", data.roster.players().values());
                context.getSource().sendSuccess(
                    () -> Component.literal("Nine Crowns (" + data.roster.players().size() + "/7): " + names),
                    false
                );
                return data.roster.players().size();
            }))
        );
    }

    public static void lightning(ServerPlayer player) {
        if (data == null || player.isSpectator()) return;

        long now = player.level().getServer().overworld().getGameTime();
        int maxCharges = Math.max(1, config.lightningMaxCharges);
        long rechargeTicks = Math.max(1, config.lightningRechargeSeconds) * 20L;
        int charges = data.lightningCharges(player.getUUID(), now, maxCharges, rechargeTicks);

        if (charges <= 0) {
            long remaining = data.lightningRemaining(player.getUUID(), now, maxCharges, rechargeTicks);
            player.sendSystemMessage(Component.literal(
                "Fulmini: 0/" + maxCharges + ". Prossima carica tra " + ((remaining + 19) / 20) + " s."
            ));
            return;
        }

        HitResult hit = ProjectileUtil.getHitResultOnViewVector(
            player,
            entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator(),
            config.lightningRange
        );

        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
            player.sendSystemMessage(Component.literal(
                "Mira a un bersaglio entro " + (int) config.lightningRange + " blocchi. Fulmini: " + charges + "/" + maxCharges + "."
            ));
            return;
        }

        if (target instanceof ServerPlayer other && !player.canHarmPlayer(other)) return;

        var level = player.level();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;

        // Consume a charge only when the lightning is actually going to fire.
        if (!data.consumeLightning(player.getUUID(), now, maxCharges, rechargeTicks)) return;

        bolt.setPos(target.position());
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        target.hurtServer(level, level.damageSources().playerAttack(player), config.lightningDamage);

        int left = data.lightningCharges(player.getUUID(), now, maxCharges, rechargeTicks);
        long remaining = data.lightningRemaining(player.getUUID(), now, maxCharges, rechargeTicks);
        String recharge = left < maxCharges
            ? " Prossima carica tra " + ((remaining + 19) / 20) + " s."
            : "";
        player.sendSystemMessage(Component.literal("Fulmini: " + left + "/" + maxCharges + "." + recharge));
    }
}
