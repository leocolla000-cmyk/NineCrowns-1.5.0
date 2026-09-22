package it.lia.ninecrowns.mixin;
import it.lia.ninecrowns.Recipes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
 @Inject(method="getRemainingItems",at=@At("HEAD"),cancellable=true)
 private void ninecrowns$consumeHeads(CraftingInput input,Level level,CallbackInfoReturnable<NonNullList<ItemStack>> cir){
  if(Recipes.opShape(input))cir.setReturnValue(NonNullList.withSize(input.size(),ItemStack.EMPTY));
 }
}
