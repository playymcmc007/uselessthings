package com.playymcmc007.uselessthings.recide;

import com.playymcmc007.uselessthings.ModItems;
import com.playymcmc007.uselessthings.ModPotions;
import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = UselessThings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrewingRecipes {

    @SubscribeEvent
    public static void registerBrewingRecipes(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 配方1：中毒II药水 + 幻之花瓣 → 混沌药水
            addAllVersionsRecipe(Potions.STRONG_POISON, ModItems.ILLUSION_PETAL.get(), ModPotions.CHAOS_POTION.get());

            // 配方2：混沌药水 + 萤石粉 → 混沌药水II
            addAllVersionsRecipe(ModPotions.CHAOS_POTION.get(), Items.GLOWSTONE_DUST, ModPotions.STRONG_CHAOS_POTION.get());

        });
    }

    private static void addAllVersionsRecipe(Potion input, ItemLike reagent, Potion output) {
        // 普通药水
        addCustomRecipe(input, Items.POTION, reagent, output, Items.POTION);
        // 喷溅药水
        addCustomRecipe(input, Items.SPLASH_POTION, reagent, output, Items.SPLASH_POTION);
        // 滞留药水
        addCustomRecipe(input, Items.LINGERING_POTION, reagent, output, Items.LINGERING_POTION);
    }

    private static void addCustomRecipe(Potion inputPotion, ItemLike inputContainer, ItemLike reagent,
                                        Potion outputPotion, ItemLike outputContainer) {
        ItemStack input = PotionUtils.setPotion(new ItemStack(inputContainer), inputPotion);
        ItemStack output = PotionUtils.setPotion(new ItemStack(outputContainer), outputPotion);
        BrewingRecipeRegistry.addRecipe(new CustomBrewingRecipe(input, reagent, output));
    }

    // 自定义酿造配方类
    private static final class CustomBrewingRecipe extends BrewingRecipe {
        private final ItemStack inputStack;

        public CustomBrewingRecipe(ItemStack input, ItemLike reagent, ItemStack output) {
            super(Ingredient.of(input), Ingredient.of(reagent), output);
            this.inputStack = input;
        }

        @Override
        public boolean isInput(ItemStack input) {
            if (ItemStack.isSameItem(input, this.inputStack)) {
                net.minecraft.world.item.alchemy.Potion inputPotion = PotionUtils.getPotion(input);
                net.minecraft.world.item.alchemy.Potion expectedPotion = PotionUtils.getPotion(this.inputStack);
                return inputPotion.equals(expectedPotion);
            }
            return false;
        }
    }
}