package com.mx_wj.fixtconstructpickaxe.core;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.ToolDefinitions;

public class EventUtils {
    public static Tier EMPTY_TIER = new Tier() {
        @Override
        public int getUses() {return 0;}
        @Override
        public float getSpeed() {return 0;}
        @Override
        public float getAttackDamageBonus() {return 0;}
        @Override
        public int getLevel() {return 0;}
        @Override
        public int getEnchantmentValue() {return 0;}
        @Override
        public Ingredient getRepairIngredient() {return null;}
    };

    public static boolean isPickaxe(Item item){
        if(item instanceof PickaxeItem){
            return true;
        }
        if(item.builtInRegistryHolder().is(ItemTags.PICKAXES)){
            return true;
        }
        return false;
    }

    public static Tier getTiered(ItemStack itemStack){
        if(itemStack.getItem() instanceof PickaxeItem pickaxeItem){
            return pickaxeItem.getTier();
        }
        if(itemStack.getItem() instanceof IModifiable){
            return getMiningTier(itemStack);
        }
        return EMPTY_TIER;
    }
    public static Tier getMiningTier(ItemStack toolStack) {
        if (toolStack.getItem() instanceof IModifiable) {
            ToolStack tool = ToolStack.from(toolStack);
            return tool.getStats().get(ToolStats.HARVEST_TIER);
        }
        return EMPTY_TIER;
    }
}
