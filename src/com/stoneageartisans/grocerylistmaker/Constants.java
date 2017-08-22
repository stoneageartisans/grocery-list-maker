/*
 * Copyright (C) 2016 William Mann
 * This file is part of the "Grocery List Maker" application.
 */

package com.stoneageartisans.grocerylistmaker;

public abstract class Constants {
    
    // Strings
    public static final String ADD = "* ADD NEW *";
    public static final String SELECT = "* SELECT *";
    
    // Enumerations
    public static enum FoodType {
        
        NONE,
        MEAT,
        PRODUCE,
        BAKERY,
        DAIRY,
        MAIN,
        DELI,
        FROZEN
    };
    
    public static enum ItemType {
        
        NONE,
        MAIN_COURSE,
        SIDE_DISH,
        OTHER
    };    
}
