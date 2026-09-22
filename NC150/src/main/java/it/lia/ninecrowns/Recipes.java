package it.lia.ninecrowns;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingInput;

public final class Recipes {
    // 3x3 layout:
    // H H H
    // H X H
    // M H M
    // H = one of the six OTHER registered player heads
    // X = center item, M = two Diamonds (sword/spear) or two Netherite Ingots (crown)
    private static final int[] HEAD_SLOTS = {0, 1, 2, 3, 5, 7};
    private static final int LEFT_MATERIAL_SLOT = 6;
    private static final int CENTER_SLOT = 4;
    private static final int RIGHT_MATERIAL_SLOT = 8;

    public static Item resultForCenter(ItemStack stack) {
        if (stack.is(Items.NETHERITE_SWORD)) return ModItems.SWORD;
        if (stack.is(Items.NETHERITE_SPEAR)) return ModItems.SPEAR;
        if (stack.is(ModItems.EMPTY_CROWN)) return ModItems.CROWN;
        return null;
    }

    private static Item requiredMaterial(ItemStack center) {
        if (center.is(ModItems.EMPTY_CROWN)) return Items.NETHERITE_INGOT;
        if (center.is(Items.NETHERITE_SWORD) || center.is(Items.NETHERITE_SPEAR)) return Items.DIAMOND;
        return null;
    }

    /** Shape-only check used by ResultSlotMixin so every ingredient is consumed. */
    public static boolean opShape(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) return false;

        ItemStack center = input.getItem(CENTER_SLOT);
        if (resultForCenter(center) == null) return false;

        Item material = requiredMaterial(center);
        if (material == null) return false;

        for (int slot : HEAD_SLOTS) {
            if (!input.getItem(slot).is(Items.PLAYER_HEAD)) return false;
        }

        return input.getItem(LEFT_MATERIAL_SLOT).is(material)
            && input.getItem(RIGHT_MATERIAL_SLOT).is(material);
    }

    public static ItemStack craft(ServerPlayer player, CraftingContainer grid) {
        if (grid.getWidth() != 3 || grid.getHeight() != 3) return ItemStack.EMPTY;

        ItemStack center = grid.getItem(CENTER_SLOT);
        Item result = resultForCenter(center);
        if (result == null) return ItemStack.EMPTY;

        Item material = requiredMaterial(center);
        if (material == null
            || !grid.getItem(LEFT_MATERIAL_SLOT).is(material)
            || !grid.getItem(RIGHT_MATERIAL_SLOT).is(material)) {
            return ItemStack.EMPTY;
        }

        List<UUID> heads = new ArrayList<>(Roster.SIZE - 1);
        for (int slot : HEAD_SLOTS) {
            ItemStack stack = grid.getItem(slot);
            if (!stack.is(Items.PLAYER_HEAD)) return ItemStack.EMPTY;

            var profile = stack.get(DataComponents.PROFILE);
            if (profile == null) return ItemStack.EMPTY;

            UUID id = profile.partialProfile().id();
            if (id == null) return ItemStack.EMPTY;
            heads.add(id);
        }

        if (NineCrowns.data == null
            || !NineCrowns.data.roster.validHeads(player.getUUID(), heads)) {
            return ItemStack.EMPTY;
        }

        return ModItems.enchanted(result, player.registryAccess());
    }
}
