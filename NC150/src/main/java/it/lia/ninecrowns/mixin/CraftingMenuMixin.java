package it.lia.ninecrowns.mixin;
import it.lia.ninecrowns.Recipes;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.*;
import net.minecraft.server.level.*;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
 @Inject(method="slotChangedCraftingGrid",at=@At("TAIL"))
 private static void ninecrowns$result(AbstractContainerMenu menu,ServerLevel level,Player player,CraftingContainer grid,ResultContainer result,RecipeHolder<CraftingRecipe> recipe,CallbackInfo ci){
  if(!(player instanceof ServerPlayer sp))return; var out=Recipes.craft(sp,grid); if(out.isEmpty())return;
  result.setItem(0,out); menu.setRemoteSlot(0,out); sp.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId,menu.incrementStateId(),0,out));
 }
}
