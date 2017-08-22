/*
 * Copyright (C) 2016 William Mann
 * This file is part of the "Grocery List Maker" application.
 */

package com.stoneageartisans.grocerylistmaker;

import com.stoneageartisans.grocerylistmaker.Constants.ItemType;
import java.util.ArrayList;

public class FoodItem {
    
    private final String name;
    private final ItemType itemType;
    private final ArrayList<Ingredient> ingredients;
    
    public FoodItem(String NAME, ItemType ITEM_TYPE, ArrayList<Ingredient> INGREDIENTS) {
        name = NAME;
        itemType = ITEM_TYPE;
        ingredients = INGREDIENTS;
    }
    
    public String getName() {
        return name;
    }
    
    public ItemType getItemType() {
        return itemType;
    }
    
    public ArrayList<Ingredient> getIngredients() {
        return ingredients;
    }
    
}
