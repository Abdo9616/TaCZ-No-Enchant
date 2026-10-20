package com.example.tacznoenchant;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("tacznoenchant")
@Mod.EventBusSubscriber(modid = "tacznoenchant")
public class TaczNoEnchant {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        boolean leftIsTaczGun = BuiltInRegistries.ITEM.getKey(left.getItem())
                .getNamespace().equals("tacz");

        boolean rightIsEnchantedBook = right.getItem() instanceof EnchantedBookItem;

        if (leftIsTaczGun && rightIsEnchantedBook) {
            event.setCanceled(true);
        }
    }
}
