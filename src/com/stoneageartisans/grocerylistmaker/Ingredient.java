/*
 * Copyright (C) 2016 William Mann
 * This file is part of the "Grocery List Maker" application.
 */

package com.stoneageartisans.grocerylistmaker;

import com.stoneageartisans.grocerylistmaker.Constants.FoodType;

public class Ingredient {
    
    private final String name;
    private final FoodType foodType;
     
    public Ingredient(String NAME, FoodType FOOD_TYPE) {
        name = NAME;
        foodType = FOOD_TYPE;
    }
    
    public String getName() {
        return name;
    }
    
    public FoodType getFoodType() {
        return foodType;
    }
    
}
