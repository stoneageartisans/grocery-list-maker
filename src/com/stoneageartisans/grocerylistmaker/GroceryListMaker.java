/*
 * Copyright (C) 2016 William Mann
 * This file is part of the "Grocery List Maker" application.
 */

package com.stoneageartisans.grocerylistmaker;

import static com.stoneageartisans.grocerylistmaker.Constants.FoodType;
import static com.stoneageartisans.grocerylistmaker.Constants.ItemType.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import static com.stoneageartisans.grocerylistmaker.Constants.ADD;
import static com.stoneageartisans.grocerylistmaker.Constants.SELECT;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.UIManager.LookAndFeelInfo;

public class GroceryListMaker extends javax.swing.JFrame {

    // Variables
    private final String newline = "\n";
    private final int SATURDAY = 0;
    private final int SUNDAY = 1;
    private final int MONDAY = 2;
    private final int TUESDAY = 3;
    private final int WEDNESDAY = 4;
    private final int THURSDAY = 5;
    private final int FRIDAY = 6;
    private final int DAYS = 7;
    private final int MAX_INGREDIENTS = 10;
    private final String[] day = { "Saturday",
                                   "Sunday",
                                   "Monday",
                                   "Tuesday",
                                   "Wednesday",
                                   "Thursday",
                                   "Friday" };
    private String[] main_course;
    private String[] side_dish1;
    private String[] side_dish2;
    private int[] people;
    private HashMap<String, FoodItem> main_courses;
    private HashMap<String, FoodItem> side_dishes;
    private ArrayList<String> main_course_choices;
    private ArrayList<String> side_dish_choices;
    private HashMap<String, Ingredient> ingredients;
    private ArrayList<Ingredient> raw_shopping_list;
    private HashMap<String, HashMap<String, Integer>> organized_shopping_list;
    private ArrayList<String> shopping_list;
    private Connection connection;
    private Statement statement;
    private ResultSet result_set;
    
    // Constructor
    public GroceryListMaker() {
        
        // Set up database connection
        connection = null;
        statement = null;
        result_set = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:grocery-list-maker.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException ex) {}
        System.out.println("Set up database connection");

        // Load ingredients from database
        ingredients = new HashMap<String, Ingredient>();
        try {
            result_set = statement.executeQuery(
                "SELECT * from ingredients"
            );
            while(result_set.next()) {
                ingredients.put(result_set.getString("NAME"),
                                new Ingredient(result_set.getString("NAME"),
                                               FoodType.valueOf(result_set.getString("FOOD_TYPE"))
                                )
                );               
            }
            result_set = null;
        } catch (SQLException ex) {}
        System.out.println("Load ingredients from database");        
        
        // Load main courses from database
        load_main_courses();
        
        // Load side dishes from database
        load_side_dishes();
        
        // Initialize GUI components
        initComponents();
        System.out.println("Initialize GUI components");
        
        // Load user settings from database
        // NOTHING YET
        
        // Load the current menu from the database
        main_course = new String[DAYS];
        side_dish1 = new String[DAYS];
        side_dish2 = new String[DAYS];
        people = new int[7];
        for(int i = 0; i < DAYS; i++) {
            main_course[i] = "NONE";
            side_dish1[i] = "NONE";
            side_dish2[i] = "NONE";
            people[i] = 0;
            try {
                result_set = statement.executeQuery(
                    "SELECT MAIN_COURSE, SIDE_DISH1, SIDE_DISH2, PEOPLE from menu " +
                        "WHERE DAY = '" + day[i] + "'"
                );
                if(result_set.next()) {
                    main_course[i] = result_set.getString("MAIN_COURSE");
                    side_dish1[i] = result_set.getString("SIDE_DISH1");
                    side_dish2[i] = result_set.getString("SIDE_DISH2");
                    people[i] = result_set.getInt("PEOPLE");
                }                    
                result_set = null;
            } catch (SQLException ex) {
                System.out.println("Query did not execute");
            }
            update_menu(i);
            System.out.println(day[i] + "'s menu loaded from database");
        }
        System.out.println("Load the current menu from the database");
        
        // Create the shopping list
        shopping_list = new ArrayList<>();
        create_shopping_list();
        
        // Set up main courses tab
        populate_available_main_courses();
        
        // Set up side dishes tab
        populate_available_side_dishes();
        
        // Set up ingredients tab
        populate_available_ingredients();        
        
        // Set the controls to the first day in the week
        populate_choices(SATURDAY);
        System.out.println("Set the controls to the first day in the week");
        
        // Set the location of the application window to the center of the screen
        this.setLocation(
                ( this.getGraphicsConfiguration().getDevice().getDisplayMode().getWidth() - this.getWidth() ) / 2,
                ( this.getGraphicsConfiguration().getDevice().getDisplayMode().getHeight() - this.getHeight() ) / 2
        );
        System.out.println("Set the location of the application window to the center of the screen");
        
    }
    
    private void update_menu(int DAY) {
                
        // Update the selected day's menu & number of people
        switch(DAY) {
            case SATURDAY:
                jTextArea_menu_saturday.setText("");
                if(!main_course[DAY].contains("NONE")) {
                    jTextArea_menu_saturday.append(main_course[DAY] + newline);
                }
                if(!side_dish1[DAY].contains("NONE")) {
                    jTextArea_menu_saturday.append(side_dish1[DAY] + newline);
                }
                if(!side_dish2[DAY].contains("NONE")) {
                    jTextArea_menu_saturday.append(side_dish2[DAY]);
                }
                jLabel_menu_saturday.setText("Saturday (" + people[DAY] + " people):");
                break;
            case SUNDAY:
                jTextArea_menu_sunday.setText("");
                if(!main_course[DAY].contains("NONE")) {
                    jTextArea_menu_sunday.append(main_course[DAY] + newline);
                }
                if(!side_dish1[DAY].contains("NONE")) {
                    jTextArea_menu_sunday.append(side_dish1[DAY] + newline);
                }
                if(!side_dish2[DAY].contains("NONE")) {
                    jTextArea_menu_sunday.append(side_dish2[DAY]);
                }
                jLabel_menu_sunday.setText("Sunday (" + people[DAY] + " people):");
                break;
            case MONDAY:
                jTextArea_menu_monday.setText("");
                if(!main_course[DAY].contains("NONE")) {
                    jTextArea_menu_monday.append(main_course[DAY] + newline);
                }
                if(!side_dish1[DAY].contains("NONE")) {
                    jTextArea_menu_monday.append(side_dish1[DAY] + newline);
                }
                if(!side_dish2[DAY].contains("NONE")) {
                    jTextArea_menu_monday.append(side_dish2[DAY]);
                }
                jLabel_menu_monday.setText("Monday (" + people[DAY] + " people):");
                break;
            case TUESDAY:
                jTextArea_menu_tuesday.setText("");
                if(!main_course[DAY].contains("NONE")) {
                    jTextArea_menu_tuesday.append(main_course[DAY] + newline);
                }
                if(!side_dish1[DAY].contains("NONE")) {
                    jTextArea_menu_tuesday.append(side_dish1[DAY] + newline);
                }
                if(!side_dish2[DAY].contains("NONE")) {
                    jTextArea_menu_tuesday.append(side_dish2[DAY]);
                }
                jLabel_menu_tuesday.setText("Tuesday (" + people[DAY] + " people):");
                break;
            case WEDNESDAY:
                jTextArea_menu_wednesday.setText("");
                if(!main_course[DAY].contains("NONE")) {
                    jTextArea_menu_wednesday.append(main_course[DAY] + newline);
                }
                if(!side_dish1[DAY].contains("NONE")) {
                    jTextArea_menu_wednesday.append(side_dish1[DAY] + newline);
                }
                if(!side_dish2[DAY].contains("NONE")) {
                    jTextArea_menu_wednesday.append(side_dish2[DAY]);
                }
                jLabel_menu_wednesday.setText("Wednesday (" + people[DAY] + " people):");
                break;
            case THURSDAY:
                jTextArea_menu_thursday.setText("");
                if(!main_course[DAY].contains("NONE")) {
                    jTextArea_menu_thursday.append(main_course[DAY] + newline);
                }
                if(!side_dish1[DAY].contains("NONE")) {
                    jTextArea_menu_thursday.append(side_dish1[DAY] + newline);
                }
                if(!side_dish2[DAY].contains("NONE")) {
                    jTextArea_menu_thursday.append(side_dish2[DAY]);
                }
                jLabel_menu_thursday.setText("Thursday (" + people[DAY] + " people):");
                break;
            case FRIDAY:
                jTextArea_menu_friday.setText("");
                if(!main_course[DAY].contains("NONE")) {
                    jTextArea_menu_friday.append(main_course[DAY] + newline);
                }
                if(!side_dish1[DAY].contains("NONE")) {
                    jTextArea_menu_friday.append(side_dish1[DAY] + newline);
                }
                if(!side_dish2[DAY].contains("NONE")) {
                    jTextArea_menu_friday.append(side_dish2[DAY]);
                }
                jLabel_menu_friday.setText("Friday (" + people[DAY] + " people):");
                break;
            default:
                break;
        }
        
    }
    
    private void update_day(int DAY) {
        
        // Update the menu in RAM
        main_course[DAY] = (String) jComboBox_modify_main_course.getSelectedItem();
        people[DAY] = (int) jSpinner_modify_people.getValue();
        side_dish1[DAY] = (String) jComboBox_modify_side1.getSelectedItem();
        side_dish2[DAY] = (String) jComboBox_modify_side2.getSelectedItem();
        
        // Update the menu in the database
        try {
            if(!connection.isValid(1)) {
                connection = DriverManager.getConnection("jdbc:sqlite:grocery-list-maker.db");
                statement = connection.createStatement();
            }
            statement.executeUpdate(
                "UPDATE menu " +
                    "SET " +
                        "MAIN_COURSE = '" + main_course[DAY] + "', " +
                        "SIDE_DISH1 = '" + side_dish1[DAY] + "', " +
                        "SIDE_DISH2 = '" + side_dish2[DAY] + "', " +
                        "SIDE_DISH3 = 'NONE', " +
                        "PEOPLE = " + people[DAY] + " " +
                    "WHERE DAY = '" + day[DAY] + "';"
            );
            System.out.println("Update executed");
            update_menu(DAY);
            create_shopping_list();
        } catch (SQLException ex) {
            System.out.println("Update did not execute");
        }
        
    }
    
    private void populate_choices(int DAY) {
        
        // Set main course combobox selection
        for(String temp_choice : main_course_choices) {
            if(temp_choice.matches(main_course[DAY])) {
                jComboBox_modify_main_course.setSelectedItem(temp_choice);
                break;
            }
        }
        
        // Set both side dish combox selections
        jSpinner_modify_people.setValue(people[DAY]);
        for(String temp_choice : side_dish_choices) {
            if(temp_choice.matches(side_dish1[DAY])) {
                jComboBox_modify_side1.setSelectedItem(temp_choice);
            }
            if(temp_choice.matches(side_dish2[DAY])) {
                jComboBox_modify_side2.setSelectedItem(temp_choice);
            }
        }
                
    }
    
    private void load_main_courses() {
    
        main_courses = new HashMap<String, FoodItem>();
        try {
            result_set = statement.executeQuery(
                "SELECT * from main_courses"
            );
            while(result_set.next()) {
                ArrayList<Ingredient> temp_ingredients = new ArrayList<Ingredient>();
                for(int i = 0; i < MAX_INGREDIENTS; i++) {                    
                    if(result_set.getString("INGREDIENT" + i) != null) {
                        temp_ingredients.add(ingredients.get(result_set.getString("INGREDIENT" + i)));
                    }
                }
                main_courses.put(result_set.getString("NAME"),
                                 new FoodItem(result_set.getString("NAME"),
                                              MAIN_COURSE,
                                              temp_ingredients
                                 )
                );                
            }
            result_set = null;
            main_course_choices = new ArrayList<String>(main_courses.keySet());
            Collections.sort(main_course_choices);
            main_course_choices.add(0, "NONE"); // Make first choice NONE
        } catch (SQLException ex) {}        
        System.out.println("Load main courses from database");
    }
    
    private void load_side_dishes() {
        
        side_dishes = new HashMap<String, FoodItem>();
        try {
            result_set = statement.executeQuery(
                "SELECT * from side_dishes"
            );
            while(result_set.next()) {
                ArrayList<Ingredient> temp_ingredients = new ArrayList<Ingredient>();
                for(int i = 0; i < MAX_INGREDIENTS; i++) {                    
                    if(result_set.getString("INGREDIENT" + i) != null) {
                        temp_ingredients.add(ingredients.get(result_set.getString("INGREDIENT" + i)));
                    }
                }
                side_dishes.put(result_set.getString("NAME"),
                                new FoodItem(result_set.getString("NAME"),
                                             SIDE_DISH,
                                             temp_ingredients
                                )
                );
            }
            result_set = null;
            // Create side dishes
            side_dish_choices = new ArrayList<String>(side_dishes.keySet());
            Collections.sort(side_dish_choices);
            side_dish_choices.add(0, "NONE"); // Make first choice NONE
        } catch (SQLException ex) {}
        System.out.println("Load side dishes from database");
    }
    
    private void create_shopping_list() {
        
        raw_shopping_list = new ArrayList<Ingredient>();
        for(int i = 0; i < DAYS; i++) {
            if(main_courses.get(main_course[i]) != null) {
                raw_shopping_list.addAll(main_courses.get(main_course[i]).getIngredients());
            }
            if(side_dishes.get(side_dish1[i]) != null) {
                raw_shopping_list.addAll(side_dishes.get(side_dish1[i]).getIngredients());
            }
            if(side_dishes.get(side_dish2[i]) != null) {
                raw_shopping_list.addAll(side_dishes.get(side_dish2[i]).getIngredients());
            }
        }
        
        organized_shopping_list = new HashMap<String, HashMap<String, Integer>>();        
        for(Ingredient temp_ingredient : raw_shopping_list) {
            String temp_type = String.valueOf(temp_ingredient.getFoodType());
            String temp_item = temp_ingredient.getName();
            if(organized_shopping_list.containsKey(temp_type)) {                
                if(organized_shopping_list.get(temp_type).containsKey(temp_item)) {
                    int temp_quantity = organized_shopping_list.get(temp_type).get(temp_item) + 1;
                    organized_shopping_list.get(temp_type).put(temp_item, temp_quantity);
                } else {
                    organized_shopping_list.get(temp_type).put(temp_item, 1);
                }
            } else {
                HashMap<String, Integer> temp_item_set = new HashMap<String, Integer>();
                temp_item_set.put(temp_item, 1);
                organized_shopping_list.put(temp_type, temp_item_set);
            }
        }
        
        populate_shopping_list();
        
    }
    
    private void populate_shopping_list() {
        
        shopping_list.clear();
        jTextArea_list.setText("");
        
        ArrayList<String> temp_list = new ArrayList<String>();
        String temp_string;
        
        // Add Dairy items
        if(organized_shopping_list.containsKey("DAIRY")) {
            shopping_list.add("DAIRY" + newline);
            temp_list.clear();
            for(String temp_item : organized_shopping_list.get("DAIRY").keySet()) {                
                if(organized_shopping_list.get("DAIRY").get(temp_item) > 1) {
                    temp_string = ("  " + temp_item + " (" + organized_shopping_list.get("DAIRY").get(temp_item) + " meals)");
                } else {
                    temp_string = ("  " + temp_item);
                }
                temp_list.add(temp_string);
            }
            Collections.sort(temp_list);
            for(String temp : temp_list) {
                shopping_list.add(temp + newline);
            }
            shopping_list.add(newline);
        }
        
        // Add Main Aisle items
        if(organized_shopping_list.containsKey("MAIN")) {
            shopping_list.add("MAIN AISLES" + newline);
            temp_list.clear();
            for(String temp_item : organized_shopping_list.get("MAIN").keySet()) {                
                if(organized_shopping_list.get("MAIN").get(temp_item) > 1) {
                    temp_string = ("  " + temp_item + " (" + organized_shopping_list.get("MAIN").get(temp_item) + " meals)");
                } else {
                    temp_string = ("  " + temp_item);
                }
                temp_list.add(temp_string);
            }
            Collections.sort(temp_list);
            for(String temp : temp_list) {
                shopping_list.add(temp + newline);
            }
            shopping_list.add(newline);
        }
        
        // Add Meat items
        if(organized_shopping_list.containsKey("MEAT")) {
            shopping_list.add("MEAT" + newline);
            temp_list.clear();
            for(String temp_item : organized_shopping_list.get("MEAT").keySet()) {            
                if(organized_shopping_list.get("MEAT").get(temp_item) > 1) {
                    temp_string = ("  " + temp_item + " (" + organized_shopping_list.get("MEAT").get(temp_item) + " meals)");
                } else {
                    temp_string = ("  " + temp_item);
                }
                temp_list.add(temp_string);
            }
            Collections.sort(temp_list);
            for(String temp : temp_list) {
                shopping_list.add(temp + newline);
            }
            shopping_list.add(newline);
        }
        
        // Add Frozen items
        if(organized_shopping_list.containsKey("FROZEN")) {
            shopping_list.add("FROZEN" + newline);
            temp_list.clear();
            for(String temp_item : organized_shopping_list.get("FROZEN").keySet()) {
                if(organized_shopping_list.get("FROZEN").get(temp_item) > 1) {
                    temp_string = ("  " + temp_item + " (" + organized_shopping_list.get("FROZEN").get(temp_item) + " meals)");
                } else {
                    temp_string = ("  " + temp_item);
                }
                temp_list.add(temp_string);
            }
            Collections.sort(temp_list);
            for(String temp : temp_list) {
                shopping_list.add(temp + newline);
            }
            shopping_list.add(newline);
        }
        
        // Add Produce items
        if(organized_shopping_list.containsKey("PRODUCE")) {
            shopping_list.add("PRODUCE" + newline);
            temp_list.clear();
            for(String temp_item : organized_shopping_list.get("PRODUCE").keySet()) {            
                if(organized_shopping_list.get("PRODUCE").get(temp_item) > 1) {
                    temp_string = ("  " + temp_item + " (" + organized_shopping_list.get("PRODUCE").get(temp_item) + " meals)");
                } else {
                    temp_string = ("  " + temp_item);
                }
                temp_list.add(temp_string);
            }
            Collections.sort(temp_list);
            for(String temp : temp_list) {
                shopping_list.add(temp + newline);
            }
            shopping_list.add(newline);
        }
        
        // Add Bakery items
        if(organized_shopping_list.containsKey("BAKERY")) {
            shopping_list.add("BAKERY" + newline);
            temp_list.clear();
            for(String temp_item : organized_shopping_list.get("BAKERY").keySet()) {
                if(organized_shopping_list.get("BAKERY").get(temp_item) > 1) {
                    temp_string = ("  " + temp_item + " (" + organized_shopping_list.get("BAKERY").get(temp_item) + " meals)");
                } else {
                    temp_string = ("  " + temp_item);
                }
                temp_list.add(temp_string);
            }
            Collections.sort(temp_list);
            for(String temp : temp_list) {
                shopping_list.add(temp + newline);
            }
            shopping_list.add(newline);
        }
        
        // Add Deli items
        if(organized_shopping_list.containsKey("DELI")) {
            shopping_list.add("DELI" + newline);
            temp_list.clear();
            for(String temp_item : organized_shopping_list.get("DELI").keySet()) {
                if(organized_shopping_list.get("DELI").get(temp_item) > 1) {
                    temp_string = ("  " + temp_item + " (" + organized_shopping_list.get("DELI").get(temp_item) + " meals)");
                } else {
                    temp_string = ("  " + temp_item);
                }
                temp_list.add(temp_string);
            }
            Collections.sort(temp_list);
            for(String temp : temp_list) {
                shopping_list.add(temp + newline);
            }
            shopping_list.add(newline);
        }
        
        // Add Non-Food items
        if(organized_shopping_list.containsKey("NONE")) {
            shopping_list.add("NON-FOOD" + newline);
            temp_list.clear();
            for(String temp_item : organized_shopping_list.get("NONE").keySet()) {
                if(organized_shopping_list.get("NONE").get(temp_item) > 1) {
                    temp_string = ("  " + temp_item + " (x" + organized_shopping_list.get("NONE").get(temp_item) + ")");
                } else {
                    temp_string = ("  " + temp_item);
                }
                temp_list.add(temp_string);
            }
            Collections.sort(temp_list);
            for(String temp : temp_list) {
                shopping_list.add(temp + newline);
            }
            shopping_list.add(newline);
        }
        
        for(String line : shopping_list) {
            jTextArea_list.append(line);
        }
        jTextArea_list.setCaretPosition(0);        
    }
    
    private void populate_available_main_courses() {
        
        jTextArea_available_main_courses.setText("");
        
        ArrayList<String> temp_list = new ArrayList<String>();        
        for(String temp : main_courses.keySet()) {
            temp_list.add(temp);
        }        
        Collections.sort(temp_list);
        
        for(String temp : temp_list) {
            jTextArea_available_main_courses.append(temp + newline);
        }        
        jTextArea_available_main_courses.setCaretPosition(0);
        
        temp_list.add(0, ADD);
        jComboBox_editing_main_courses.setModel(new javax.swing.DefaultComboBoxModel(temp_list.toArray()));
        
        // Re-create main courses
        main_course_choices = new ArrayList<String>(main_courses.keySet());
        Collections.sort(main_course_choices);
        main_course_choices.add(0, "NONE"); // Make first choice NONE
        
        jComboBox_modify_main_course.setModel(new javax.swing.DefaultComboBoxModel(main_course_choices.toArray()));
    }
    
    private void populate_available_side_dishes() {
        
        jTextArea_available_side_dishes.setText("");
        
        ArrayList<String> temp_list = new ArrayList<String>();        
        for(String temp : side_dishes.keySet()) {
            temp_list.add(temp);
        }        
        Collections.sort(temp_list);
        
        for(String temp : temp_list) {
            jTextArea_available_side_dishes.append(temp + newline);
        }        
        jTextArea_available_side_dishes.setCaretPosition(0);
        
        temp_list.add(0, ADD);
        jComboBox_editing_side_dishes.setModel(new javax.swing.DefaultComboBoxModel(temp_list.toArray()));
        
        // Re-create side dishes
        side_dish_choices = new ArrayList<String>(side_dishes.keySet());
        Collections.sort(side_dish_choices);
        side_dish_choices.add(0, "NONE"); // Make first choice NONE
        
        jComboBox_modify_side1.setModel(new javax.swing.DefaultComboBoxModel(side_dish_choices.toArray()));
        jComboBox_modify_side2.setModel(new javax.swing.DefaultComboBoxModel(side_dish_choices.toArray()));
    }
    
    private void populate_available_ingredients() {
        
        jTextArea_available_ingredients.setText("");
        
        ArrayList<String> temp_list = new ArrayList<String>();        
        for(String temp : ingredients.keySet()) {
            temp_list.add(temp);
        }        
        Collections.sort(temp_list);
        
        for(String temp : temp_list) {
            jTextArea_available_ingredients.append(temp + newline);
        }        
        jTextArea_available_ingredients.setCaretPosition(0);
        
        temp_list.add(0, ADD);        
        jComboBox_editing_ingredients.setModel(new javax.swing.DefaultComboBoxModel(temp_list.toArray()));
        
        temp_list.remove(0);
        temp_list.add(0, SELECT);        
        Object[] temp_array = temp_list.toArray();
        
		for(int i = 0; i < MAX_INGREDIENTS; i ++) {
			jComboBox_editing_side_dish_ingredient[i].setModel(new javax.swing.DefaultComboBoxModel(temp_array));
            jComboBox_editing_main_course_ingredient[i].setModel(new javax.swing.DefaultComboBoxModel(temp_array));
		}
    }
    
    private void populate_editing_ingredients_info(String CHOICE) {
        
        jTextField_editing_ingredient_name.setText(CHOICE);
        jComboBox_editing_ingredient_type.setSelectedItem(ingredients.get(CHOICE).getFoodType());
    }
    
    private void populate_editing_side_dishes_info(String CHOICE) {
        
        jTextField_editing_side_dish_name.setText(CHOICE);
        
        for(int i = 0, max = side_dishes.get(CHOICE).getIngredients().size(); i < MAX_INGREDIENTS; i++) {
            if(i < max) {
                jComboBox_editing_side_dish_ingredient[i].setSelectedItem(side_dishes.get(CHOICE).getIngredients().get(i).getName());
            } else {
                jComboBox_editing_side_dish_ingredient[i].setSelectedIndex(0);
            }
        }
    }
    
    private void populate_editing_main_courses_info(String CHOICE) {
        
        jTextField_editing_main_course_name.setText(CHOICE);
        
        for(int i = 0, max = main_courses.get(CHOICE).getIngredients().size(); i < MAX_INGREDIENTS; i++) {
            if(i < max) {
                jComboBox_editing_main_course_ingredient[i].setSelectedItem(main_courses.get(CHOICE).getIngredients().get(i).getName());
            } else {
                jComboBox_editing_main_course_ingredient[i].setSelectedIndex(0);
            }
        }
    }
    
    private void update_main_courses_ingredients(String OLD_NAME, String NEW_NAME) {
        
        for(int i = 0; i < MAX_INGREDIENTS; i++) {
            String temp_column = "INGREDIENT" + i;
            try {
                statement.executeUpdate(                        
                    "UPDATE main_courses " +
                        "SET " + temp_column + " = REPLACE(" + temp_column + ", '" + OLD_NAME + "', '" + NEW_NAME + "') " +
                        "WHERE " + temp_column + " LIKE '" + OLD_NAME + "';"
                );
                if(i == MAX_INGREDIENTS - 1) {
                    System.out.println("Ingredient '" + NEW_NAME + "' updated in main_courses table");
                }
            } catch (SQLException ex) {}
        }
        load_main_courses();        
    }
    
    private void update_main_courses_ingredients(String NAME) {
        
        for(int i = 0; i < MAX_INGREDIENTS; i++) {
            String temp_column = "INGREDIENT" + i;
            try {
                statement.executeUpdate(                        
                    "UPDATE main_courses " +
                        "SET " + temp_column + " = REPLACE(" + temp_column + ", '" + NAME + "', " + null + ") " +
                        "WHERE " + temp_column + " LIKE '" + NAME + "';"
                );
                if(i == MAX_INGREDIENTS - 1) {
                    System.out.println("Ingredient '" + NAME + "' removed from main_courses table");
                }
            } catch (SQLException ex) {}
        }
        load_main_courses();        
    }
    
    private void update_side_dishes_ingredients(String OLD_NAME, String NEW_NAME) {
        
        for(int i = 0; i < MAX_INGREDIENTS; i++) {
            String temp_column = "INGREDIENT" + i;
            try {
                statement.executeUpdate(                        
                    "UPDATE side_dishes " +
                        "SET " + temp_column + " = REPLACE(" + temp_column + ", '" + OLD_NAME + "', '" + NEW_NAME + "') " +
                        "WHERE " + temp_column + " LIKE '" + OLD_NAME + "';"
                );
                if(i == MAX_INGREDIENTS - 1) {
                    System.out.println("Ingredient '" + NEW_NAME + "' updated in side_dishes table");
                }
            } catch (SQLException ex) {}
        }
        load_side_dishes();
    }
    
    private void update_side_dishes_ingredients(String NAME) {
        
        for(int i = 0; i < MAX_INGREDIENTS; i++) {
            String temp_column = "INGREDIENT" + i;
            try {
                statement.executeUpdate(                        
                    "UPDATE side_dishes " +
                        "SET " + temp_column + " = REPLACE(" + temp_column + ", '" + NAME + "', " + null + ") " +
                        "WHERE " + temp_column + " LIKE '" + NAME + "';"
                );
                if(i == MAX_INGREDIENTS - 1) {
                    System.out.println("Ingredient '" + NAME + "' removed from side_dishes table");
                }
            } catch (SQLException ex) {}
        }
        load_side_dishes();
    }
    
    private void remove_side_dish_from_menu(String SIDE_DISH_NAME) {
        
        try {
            statement.executeUpdate(                        
                "UPDATE menu " +
                    "SET SIDE_DISH1 = 'NONE' " +
                    "WHERE SIDE_DISH1 LIKE '" + SIDE_DISH_NAME + "';"
            );
            statement.executeUpdate(                        
                "UPDATE menu " +
                    "SET SIDE_DISH2 = 'NONE' " +
                    "WHERE SIDE_DISH2 LIKE '" + SIDE_DISH_NAME + "';"
            );
            System.out.println("Side Dish '" + SIDE_DISH_NAME + "' removed from menu table");
        } catch (SQLException ex) {}
        for(int i = 0; i < DAYS; i++) {
            if(side_dish1[i].matches(SIDE_DISH_NAME)) {
                side_dish1[i] = "NONE";
            }
            if(side_dish2[i].matches(SIDE_DISH_NAME)) {
                side_dish2[i] = "NONE";
            }
            update_menu(i);
        }   
    }
    
    private void remove_main_course_from_menu(String MAIN_COURSE_NAME) {
        
        try {
            statement.executeUpdate(                        
                "UPDATE menu " +
                    "SET MAIN_COURSE = 'NONE' " +
                    "WHERE MAIN_COURSE LIKE '" + MAIN_COURSE_NAME + "';"
            );
            System.out.println("Main Course '" + MAIN_COURSE_NAME + "' removed from menu table");
        } catch (SQLException ex) {}
        for(int i = 0; i < DAYS; i++) {
            if(main_course[i].matches(MAIN_COURSE_NAME)) {
                main_course[i] = "NONE";
            }
            update_menu(i);
        }   
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel_top = new javax.swing.JPanel();
        jTabbedPane_tabs = new javax.swing.JTabbedPane();
        jPanel_main = new javax.swing.JPanel();
        jPanel_modify = new javax.swing.JPanel();
        jLabel_modify_day = new javax.swing.JLabel();
        jComboBox_modify_day = new javax.swing.JComboBox();
        jLabel_modify_main_course = new javax.swing.JLabel();
        jComboBox_modify_main_course = new javax.swing.JComboBox();
        jLabel_modify_side1 = new javax.swing.JLabel();
        jComboBox_modify_side1 = new javax.swing.JComboBox();
        jLabel_modify_side2 = new javax.swing.JLabel();
        jComboBox_modify_side2 = new javax.swing.JComboBox();
        jLabel_modify_people = new javax.swing.JLabel();
        jSpinner_modify_people = new javax.swing.JSpinner();
        jButton_modify_update = new javax.swing.JButton();
        jPanel_menu = new javax.swing.JPanel();
        jLabel_menu_saturday = new javax.swing.JLabel();
        jScrollPane_menu_saturday = new javax.swing.JScrollPane();
        jTextArea_menu_saturday = new javax.swing.JTextArea();
        jLabel_menu_sunday = new javax.swing.JLabel();
        jScrollPane_menu_sunday = new javax.swing.JScrollPane();
        jTextArea_menu_sunday = new javax.swing.JTextArea();
        jLabel_menu_monday = new javax.swing.JLabel();
        jScrollPane_menu_monday = new javax.swing.JScrollPane();
        jTextArea_menu_monday = new javax.swing.JTextArea();
        jLabel_menu_tuesday = new javax.swing.JLabel();
        jScrollPane_menu_tuesday = new javax.swing.JScrollPane();
        jTextArea_menu_tuesday = new javax.swing.JTextArea();
        jLabel_menu_wednesday = new javax.swing.JLabel();
        jScrollPane_menu_wednesday = new javax.swing.JScrollPane();
        jTextArea_menu_wednesday = new javax.swing.JTextArea();
        jLabel_menu_thursday = new javax.swing.JLabel();
        jScrollPane_menu_thursday = new javax.swing.JScrollPane();
        jTextArea_menu_thursday = new javax.swing.JTextArea();
        jLabel_menu_friday = new javax.swing.JLabel();
        jScrollPane_menu_friday = new javax.swing.JScrollPane();
        jTextArea_menu_friday = new javax.swing.JTextArea();
        jButton_export_menu = new javax.swing.JButton();
        jPanel_list = new javax.swing.JPanel();
        jScrollPane_list = new javax.swing.JScrollPane();
        jTextArea_list = new javax.swing.JTextArea();
        jButton_export_shopping_list = new javax.swing.JButton();
        jPanel_main_courses = new javax.swing.JPanel();
        jPanel_editing_main_courses = new javax.swing.JPanel();
        jLabel_editing_main_course = new javax.swing.JLabel();
        jComboBox_editing_main_courses = new javax.swing.JComboBox<>();
        jLabel_editing_main_course_name = new javax.swing.JLabel();
        jTextField_editing_main_course_name = new javax.swing.JTextField();
        jLabel_editing_main_course_ingredients = new javax.swing.JLabel();
		jComboBox_editing_main_course_ingredient = new javax.swing.JComboBox[MAX_INGREDIENTS];
        jComboBox_editing_main_course_ingredient[0] = new javax.swing.JComboBox<>();
        jComboBox_editing_main_course_ingredient[5] = new javax.swing.JComboBox<>();
        jComboBox_editing_main_course_ingredient[1] = new javax.swing.JComboBox<>();
        jComboBox_editing_main_course_ingredient[6] = new javax.swing.JComboBox<>();
        jComboBox_editing_main_course_ingredient[2] = new javax.swing.JComboBox<>();
        jComboBox_editing_main_course_ingredient[7] = new javax.swing.JComboBox<>();
        jComboBox_editing_main_course_ingredient[3] = new javax.swing.JComboBox<>();
        jComboBox_editing_main_course_ingredient[8] = new javax.swing.JComboBox<>();
        jComboBox_editing_main_course_ingredient[4] = new javax.swing.JComboBox<>();
        jComboBox_editing_main_course_ingredient[9] = new javax.swing.JComboBox<>();
        jPanel_editing_main_course_actions = new javax.swing.JPanel();
        jButton_editing_main_course_add = new javax.swing.JButton();
        jButton_editing_main_course_remove = new javax.swing.JButton();
        jButton_editing_main_course_update = new javax.swing.JButton();
        jPanel_available_main_courses = new javax.swing.JPanel();
        jScrollPane_current_ingredients1 = new javax.swing.JScrollPane();
        jTextArea_available_main_courses = new javax.swing.JTextArea();
        jPanel_side_dishes = new javax.swing.JPanel();
        jPanel_editing_side_dishes = new javax.swing.JPanel();
        jLabel_editing_side_dish = new javax.swing.JLabel();
        jComboBox_editing_side_dishes = new javax.swing.JComboBox<>();
        jLabel_editing_side_dish_name = new javax.swing.JLabel();
        jTextField_editing_side_dish_name = new javax.swing.JTextField();
        jPanel_editing_side_dish_actions = new javax.swing.JPanel();
        jButton_editing_side_dish_add = new javax.swing.JButton();
        jButton_editing_side_dish_remove = new javax.swing.JButton();
        jButton_editing_side_dish_update = new javax.swing.JButton();
        jLabel_editing_side_dish_ingredients = new javax.swing.JLabel();
		jComboBox_editing_side_dish_ingredient = new javax.swing.JComboBox[MAX_INGREDIENTS];
        jComboBox_editing_side_dish_ingredient[0] = new javax.swing.JComboBox<>();
        jComboBox_editing_side_dish_ingredient[1] = new javax.swing.JComboBox<>();
        jComboBox_editing_side_dish_ingredient[2] = new javax.swing.JComboBox<>();
        jComboBox_editing_side_dish_ingredient[3] = new javax.swing.JComboBox<>();
        jComboBox_editing_side_dish_ingredient[4] = new javax.swing.JComboBox<>();
        jComboBox_editing_side_dish_ingredient[5] = new javax.swing.JComboBox<>();
        jComboBox_editing_side_dish_ingredient[6] = new javax.swing.JComboBox<>();
        jComboBox_editing_side_dish_ingredient[7] = new javax.swing.JComboBox<>();
        jComboBox_editing_side_dish_ingredient[8] = new javax.swing.JComboBox<>();
        jComboBox_editing_side_dish_ingredient[9] = new javax.swing.JComboBox<>();
        jPanel_available_side_dishes = new javax.swing.JPanel();
        jScrollPane_current_ingredients2 = new javax.swing.JScrollPane();
        jTextArea_available_side_dishes = new javax.swing.JTextArea();
        jPanel_ingredients = new javax.swing.JPanel();
        jPanel_editing_ingredients = new javax.swing.JPanel();
        jLabel_editing_ingredient = new javax.swing.JLabel();
        jComboBox_editing_ingredients = new javax.swing.JComboBox<>();
        jLabel_editing_ingredient_name = new javax.swing.JLabel();
        jTextField_editing_ingredient_name = new javax.swing.JTextField();
        jLabel_editing_ingredient_type = new javax.swing.JLabel();
        jComboBox_editing_ingredient_type = new javax.swing.JComboBox<>();
        jPanel_editing_ingredients_actions = new javax.swing.JPanel();
        jButton_editing_ingredients_add = new javax.swing.JButton();
        jButton_editing_ingredients_remove = new javax.swing.JButton();
        jButton_editing_ingredients_update = new javax.swing.JButton();
        jPanel_available_ingredients = new javax.swing.JPanel();
        jScrollPane_current_ingredients3 = new javax.swing.JScrollPane();
        jTextArea_available_ingredients = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Grocery List Maker");

        jPanel_modify.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "MODIFY THE MENU", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jLabel_modify_day.setText("Day of Week:");

        jComboBox_modify_day.setModel(new DefaultComboBoxModel(day));
        jComboBox_modify_day.setSelectedIndex(SATURDAY);
        jComboBox_modify_day.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox_modify_dayItemStateChanged(evt);
            }
        });

        jLabel_modify_main_course.setText("Main Course:");

        jComboBox_modify_main_course.setModel(new DefaultComboBoxModel(main_course_choices.toArray()));

        jLabel_modify_side1.setText("Optional Side 1:");

        jComboBox_modify_side1.setModel(new javax.swing.DefaultComboBoxModel( side_dish_choices.toArray() ));

        jLabel_modify_side2.setText("Optional Side 2:");

        jComboBox_modify_side2.setModel(new javax.swing.DefaultComboBoxModel( side_dish_choices.toArray() ));

        jLabel_modify_people.setText("Number of People:");

        jSpinner_modify_people.setModel(new javax.swing.SpinnerNumberModel(4, 0, 99, 1));

        jButton_modify_update.setText("Update Menu");
        jButton_modify_update.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_modify_updateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_modifyLayout = new javax.swing.GroupLayout(jPanel_modify);
        jPanel_modify.setLayout(jPanel_modifyLayout);
        jPanel_modifyLayout.setHorizontalGroup(
            jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_modifyLayout.createSequentialGroup()
                .addGroup(jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_modifyLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel_modify_people, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel_modify_side2, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel_modify_side1, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel_modify_main_course, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel_modify_day, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBox_modify_side2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox_modify_day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox_modify_main_course, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox_modify_side1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSpinner_modify_people, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel_modifyLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton_modify_update)))
                .addContainerGap())
        );
        jPanel_modifyLayout.setVerticalGroup(
            jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_modifyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel_modify_day)
                    .addComponent(jComboBox_modify_day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel_modify_main_course)
                    .addComponent(jComboBox_modify_main_course, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel_modify_side1)
                    .addComponent(jComboBox_modify_side1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel_modify_side2)
                    .addComponent(jComboBox_modify_side2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel_modifyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel_modify_people)
                    .addComponent(jSpinner_modify_people, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jButton_modify_update)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel_menu.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "CURRENT MENU", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jLabel_menu_saturday.setText("Saturday:");

        jTextArea_menu_saturday.setEditable(false);
        jTextArea_menu_saturday.setColumns(30);
        jTextArea_menu_saturday.setLineWrap(true);
        jTextArea_menu_saturday.setRows(3);
        jTextArea_menu_saturday.setTabSize(4);
        jTextArea_menu_saturday.setWrapStyleWord(true);
        jScrollPane_menu_saturday.setViewportView(jTextArea_menu_saturday);

        jLabel_menu_sunday.setText("Sunday:");

        jTextArea_menu_sunday.setEditable(false);
        jTextArea_menu_sunday.setColumns(30);
        jTextArea_menu_sunday.setLineWrap(true);
        jTextArea_menu_sunday.setRows(3);
        jTextArea_menu_sunday.setTabSize(4);
        jTextArea_menu_sunday.setWrapStyleWord(true);
        jScrollPane_menu_sunday.setViewportView(jTextArea_menu_sunday);

        jLabel_menu_monday.setText("Monday:");

        jTextArea_menu_monday.setEditable(false);
        jTextArea_menu_monday.setColumns(30);
        jTextArea_menu_monday.setLineWrap(true);
        jTextArea_menu_monday.setRows(3);
        jTextArea_menu_monday.setTabSize(4);
        jTextArea_menu_monday.setWrapStyleWord(true);
        jScrollPane_menu_monday.setViewportView(jTextArea_menu_monday);

        jLabel_menu_tuesday.setText("Tuesday:");

        jTextArea_menu_tuesday.setEditable(false);
        jTextArea_menu_tuesday.setColumns(30);
        jTextArea_menu_tuesday.setLineWrap(true);
        jTextArea_menu_tuesday.setRows(3);
        jTextArea_menu_tuesday.setTabSize(4);
        jTextArea_menu_tuesday.setWrapStyleWord(true);
        jScrollPane_menu_tuesday.setViewportView(jTextArea_menu_tuesday);

        jLabel_menu_wednesday.setText("Wednesday:");

        jTextArea_menu_wednesday.setEditable(false);
        jTextArea_menu_wednesday.setColumns(30);
        jTextArea_menu_wednesday.setLineWrap(true);
        jTextArea_menu_wednesday.setRows(3);
        jTextArea_menu_wednesday.setTabSize(4);
        jTextArea_menu_wednesday.setWrapStyleWord(true);
        jScrollPane_menu_wednesday.setViewportView(jTextArea_menu_wednesday);

        jLabel_menu_thursday.setText("Thursday:");

        jTextArea_menu_thursday.setEditable(false);
        jTextArea_menu_thursday.setColumns(30);
        jTextArea_menu_thursday.setLineWrap(true);
        jTextArea_menu_thursday.setRows(3);
        jTextArea_menu_thursday.setTabSize(4);
        jTextArea_menu_thursday.setWrapStyleWord(true);
        jScrollPane_menu_thursday.setViewportView(jTextArea_menu_thursday);

        jLabel_menu_friday.setText("Friday:");

        jTextArea_menu_friday.setEditable(false);
        jTextArea_menu_friday.setColumns(30);
        jTextArea_menu_friday.setLineWrap(true);
        jTextArea_menu_friday.setRows(3);
        jTextArea_menu_friday.setTabSize(4);
        jTextArea_menu_friday.setWrapStyleWord(true);
        jScrollPane_menu_friday.setViewportView(jTextArea_menu_friday);

        jButton_export_menu.setText("Export to File");
        jButton_export_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_export_menuActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_menuLayout = new javax.swing.GroupLayout(jPanel_menu);
        jPanel_menu.setLayout(jPanel_menuLayout);
        jPanel_menuLayout.setHorizontalGroup(
            jPanel_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_menuLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_menuLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel_menuLayout.createSequentialGroup()
                                .addComponent(jScrollPane_menu_friday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton_export_menu))
                            .addComponent(jScrollPane_menu_thursday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane_menu_tuesday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane_menu_monday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane_menu_sunday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane_menu_saturday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane_menu_wednesday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel_menu_saturday)
                        .addComponent(jLabel_menu_sunday)
                        .addComponent(jLabel_menu_monday)
                        .addComponent(jLabel_menu_tuesday)
                        .addComponent(jLabel_menu_thursday)
                        .addComponent(jLabel_menu_friday)
                        .addComponent(jLabel_menu_wednesday)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_menuLayout.setVerticalGroup(
            jPanel_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_menuLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton_export_menu)
                    .addGroup(jPanel_menuLayout.createSequentialGroup()
                        .addComponent(jLabel_menu_saturday)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane_menu_saturday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel_menu_sunday)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane_menu_sunday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel_menu_monday)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane_menu_monday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel_menu_tuesday)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane_menu_tuesday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel_menu_wednesday)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane_menu_wednesday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel_menu_thursday)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane_menu_thursday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel_menu_friday)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane_menu_friday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel_list.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "SHOPPING LIST", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jTextArea_list.setEditable(false);
        jTextArea_list.setColumns(30);
        jTextArea_list.setRows(15);
        jScrollPane_list.setViewportView(jTextArea_list);

        jButton_export_shopping_list.setText("Export to File");
        jButton_export_shopping_list.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_export_shopping_listActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_listLayout = new javax.swing.GroupLayout(jPanel_list);
        jPanel_list.setLayout(jPanel_listLayout);
        jPanel_listLayout.setHorizontalGroup(
            jPanel_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel_listLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton_export_shopping_list)
                .addContainerGap())
            .addGroup(jPanel_listLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane_list, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_listLayout.setVerticalGroup(
            jPanel_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_listLayout.createSequentialGroup()
                .addComponent(jScrollPane_list)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton_export_shopping_list)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel_mainLayout = new javax.swing.GroupLayout(jPanel_main);
        jPanel_main.setLayout(jPanel_mainLayout);
        jPanel_mainLayout.setHorizontalGroup(
            jPanel_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_mainLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_menu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanel_modify, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel_list, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_mainLayout.setVerticalGroup(
            jPanel_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_mainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel_menu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel_mainLayout.createSequentialGroup()
                        .addComponent(jPanel_modify, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel_list, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane_tabs.addTab("Menu & Shopping List", jPanel_main);

        jPanel_editing_main_courses.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "EDIT MAIN COURSES", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jLabel_editing_main_course.setText("Select:");

        jComboBox_editing_main_courses.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox_editing_main_coursesItemStateChanged(evt);
            }
        });

        jLabel_editing_main_course_name.setText("Name:");

        jTextField_editing_main_course_name.setColumns(15);

        jLabel_editing_main_course_ingredients.setText("Ingredients:");

        jPanel_editing_main_course_actions.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jButton_editing_main_course_add.setText("ADD");
        jButton_editing_main_course_add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_editing_main_course_addActionPerformed(evt);
            }
        });

        jButton_editing_main_course_remove.setText("REMOVE");
        jButton_editing_main_course_remove.setEnabled(false);
        jButton_editing_main_course_remove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_editing_main_course_removeActionPerformed(evt);
            }
        });

        jButton_editing_main_course_update.setText("UPDATE");
        jButton_editing_main_course_update.setEnabled(false);
        jButton_editing_main_course_update.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_editing_main_course_updateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_editing_main_course_actionsLayout = new javax.swing.GroupLayout(jPanel_editing_main_course_actions);
        jPanel_editing_main_course_actions.setLayout(jPanel_editing_main_course_actionsLayout);
        jPanel_editing_main_course_actionsLayout.setHorizontalGroup(
            jPanel_editing_main_course_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_main_course_actionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_editing_main_course_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addGroup(jPanel_editing_main_course_actionsLayout.createSequentialGroup()
                        .addComponent(jButton_editing_main_course_update, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton_editing_main_course_remove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jButton_editing_main_course_add, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel_editing_main_course_actionsLayout.setVerticalGroup(
            jPanel_editing_main_course_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_main_course_actionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton_editing_main_course_add)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_main_course_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jButton_editing_main_course_update)
                    .addComponent(jButton_editing_main_course_remove))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel_editing_main_coursesLayout = new javax.swing.GroupLayout(jPanel_editing_main_courses);
        jPanel_editing_main_courses.setLayout(jPanel_editing_main_coursesLayout);
        jPanel_editing_main_coursesLayout.setHorizontalGroup(
            jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_main_coursesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel_editing_main_course, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel_editing_main_course_name, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel_editing_main_course_ingredients, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_editing_main_coursesLayout.createSequentialGroup()
                        .addComponent(jComboBox_editing_main_course_ingredient[4], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_editing_main_course_ingredient[9], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel_editing_main_coursesLayout.createSequentialGroup()
                        .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBox_editing_main_courses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField_editing_main_course_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel_editing_main_coursesLayout.createSequentialGroup()
                                .addComponent(jComboBox_editing_main_course_ingredient[0], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox_editing_main_course_ingredient[5], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel_editing_main_course_actions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel_editing_main_coursesLayout.createSequentialGroup()
                        .addComponent(jComboBox_editing_main_course_ingredient[1], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_editing_main_course_ingredient[6], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel_editing_main_coursesLayout.createSequentialGroup()
                        .addComponent(jComboBox_editing_main_course_ingredient[2], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_editing_main_course_ingredient[7], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel_editing_main_coursesLayout.createSequentialGroup()
                        .addComponent(jComboBox_editing_main_course_ingredient[3], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_editing_main_course_ingredient[8], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_editing_main_coursesLayout.setVerticalGroup(
            jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_main_coursesLayout.createSequentialGroup()
                .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_editing_main_coursesLayout.createSequentialGroup()
                        .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel_editing_main_course)
                            .addComponent(jComboBox_editing_main_courses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel_editing_main_course_name)
                            .addComponent(jTextField_editing_main_course_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel_editing_main_course_ingredients)
                            .addComponent(jComboBox_editing_main_course_ingredient[0], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox_editing_main_course_ingredient[5], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jPanel_editing_main_course_actions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_editing_main_course_ingredient[1], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_editing_main_course_ingredient[6], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_editing_main_course_ingredient[2], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_editing_main_course_ingredient[7], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_editing_main_course_ingredient[3], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_editing_main_course_ingredient[8], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_editing_main_course_ingredient[4], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_editing_main_course_ingredient[9], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );

        jPanel_available_main_courses.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "AVAILABLE MAIN COURSES", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jTextArea_available_main_courses.setEditable(false);
        jTextArea_available_main_courses.setColumns(30);
        jTextArea_available_main_courses.setRows(15);
        jScrollPane_current_ingredients1.setViewportView(jTextArea_available_main_courses);

        javax.swing.GroupLayout jPanel_available_main_coursesLayout = new javax.swing.GroupLayout(jPanel_available_main_courses);
        jPanel_available_main_courses.setLayout(jPanel_available_main_coursesLayout);
        jPanel_available_main_coursesLayout.setHorizontalGroup(
            jPanel_available_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_available_main_coursesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane_current_ingredients1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_available_main_coursesLayout.setVerticalGroup(
            jPanel_available_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_available_main_coursesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane_current_ingredients1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel_main_coursesLayout = new javax.swing.GroupLayout(jPanel_main_courses);
        jPanel_main_courses.setLayout(jPanel_main_coursesLayout);
        jPanel_main_coursesLayout.setHorizontalGroup(
            jPanel_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_main_coursesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanel_available_main_courses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel_editing_main_courses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_main_coursesLayout.setVerticalGroup(
            jPanel_main_coursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_main_coursesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_editing_main_courses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel_available_main_courses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane_tabs.addTab("Main Courses", jPanel_main_courses);

        jPanel_editing_side_dishes.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "EDIT SIDE DISHES", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jLabel_editing_side_dish.setText("Select:");

        jComboBox_editing_side_dishes.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox_editing_side_dishesItemStateChanged(evt);
            }
        });

        jLabel_editing_side_dish_name.setText("Name:");

        jTextField_editing_side_dish_name.setColumns(15);

        jPanel_editing_side_dish_actions.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jButton_editing_side_dish_add.setText("ADD");
        jButton_editing_side_dish_add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_editing_side_dish_addActionPerformed(evt);
            }
        });

        jButton_editing_side_dish_remove.setText("REMOVE");
        jButton_editing_side_dish_remove.setEnabled(false);
        jButton_editing_side_dish_remove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_editing_side_dish_removeActionPerformed(evt);
            }
        });

        jButton_editing_side_dish_update.setText("UPDATE");
        jButton_editing_side_dish_update.setEnabled(false);
        jButton_editing_side_dish_update.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_editing_side_dish_updateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_editing_side_dish_actionsLayout = new javax.swing.GroupLayout(jPanel_editing_side_dish_actions);
        jPanel_editing_side_dish_actions.setLayout(jPanel_editing_side_dish_actionsLayout);
        jPanel_editing_side_dish_actionsLayout.setHorizontalGroup(
            jPanel_editing_side_dish_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_side_dish_actionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_editing_side_dish_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addGroup(jPanel_editing_side_dish_actionsLayout.createSequentialGroup()
                        .addComponent(jButton_editing_side_dish_update, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton_editing_side_dish_remove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jButton_editing_side_dish_add, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel_editing_side_dish_actionsLayout.setVerticalGroup(
            jPanel_editing_side_dish_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_side_dish_actionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton_editing_side_dish_add)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_side_dish_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jButton_editing_side_dish_update)
                    .addComponent(jButton_editing_side_dish_remove))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel_editing_side_dish_ingredients.setText("Ingredients:");

        javax.swing.GroupLayout jPanel_editing_side_dishesLayout = new javax.swing.GroupLayout(jPanel_editing_side_dishes);
        jPanel_editing_side_dishes.setLayout(jPanel_editing_side_dishesLayout);
        jPanel_editing_side_dishesLayout.setHorizontalGroup(
            jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_side_dishesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel_editing_side_dish, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel_editing_side_dish_name, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel_editing_side_dish_ingredients, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_editing_side_dishesLayout.createSequentialGroup()
                        .addComponent(jComboBox_editing_side_dish_ingredient[4], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_editing_side_dish_ingredient[9], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel_editing_side_dishesLayout.createSequentialGroup()
                        .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBox_editing_side_dishes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField_editing_side_dish_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel_editing_side_dishesLayout.createSequentialGroup()
                                .addComponent(jComboBox_editing_side_dish_ingredient[0], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox_editing_side_dish_ingredient[5], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel_editing_side_dish_actions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel_editing_side_dishesLayout.createSequentialGroup()
                        .addComponent(jComboBox_editing_side_dish_ingredient[1], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_editing_side_dish_ingredient[6], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel_editing_side_dishesLayout.createSequentialGroup()
                        .addComponent(jComboBox_editing_side_dish_ingredient[2], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_editing_side_dish_ingredient[7], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel_editing_side_dishesLayout.createSequentialGroup()
                        .addComponent(jComboBox_editing_side_dish_ingredient[3], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox_editing_side_dish_ingredient[8], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_editing_side_dishesLayout.setVerticalGroup(
            jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_side_dishesLayout.createSequentialGroup()
                .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_editing_side_dishesLayout.createSequentialGroup()
                        .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel_editing_side_dish)
                            .addComponent(jComboBox_editing_side_dishes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel_editing_side_dish_name)
                            .addComponent(jTextField_editing_side_dish_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel_editing_side_dish_ingredients)
                            .addComponent(jComboBox_editing_side_dish_ingredient[0], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox_editing_side_dish_ingredient[5], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jPanel_editing_side_dish_actions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_editing_side_dish_ingredient[1], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_editing_side_dish_ingredient[6], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_editing_side_dish_ingredient[2], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_editing_side_dish_ingredient[7], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_editing_side_dish_ingredient[3], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_editing_side_dish_ingredient[8], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox_editing_side_dish_ingredient[4], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_editing_side_dish_ingredient[9], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );

        jPanel_available_side_dishes.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "AVAILABLE SIDE DISHES", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jTextArea_available_side_dishes.setEditable(false);
        jTextArea_available_side_dishes.setColumns(30);
        jTextArea_available_side_dishes.setRows(15);
        jScrollPane_current_ingredients2.setViewportView(jTextArea_available_side_dishes);

        javax.swing.GroupLayout jPanel_available_side_dishesLayout = new javax.swing.GroupLayout(jPanel_available_side_dishes);
        jPanel_available_side_dishes.setLayout(jPanel_available_side_dishesLayout);
        jPanel_available_side_dishesLayout.setHorizontalGroup(
            jPanel_available_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_available_side_dishesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane_current_ingredients2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_available_side_dishesLayout.setVerticalGroup(
            jPanel_available_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_available_side_dishesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane_current_ingredients2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel_side_dishesLayout = new javax.swing.GroupLayout(jPanel_side_dishes);
        jPanel_side_dishes.setLayout(jPanel_side_dishesLayout);
        jPanel_side_dishesLayout.setHorizontalGroup(
            jPanel_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_side_dishesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanel_editing_side_dishes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel_available_side_dishes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_side_dishesLayout.setVerticalGroup(
            jPanel_side_dishesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_side_dishesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_editing_side_dishes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel_available_side_dishes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane_tabs.addTab("Side Dishes", jPanel_side_dishes);

        jPanel_editing_ingredients.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "EDIT INGREDIENTS", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jLabel_editing_ingredient.setText("Select:");

        jComboBox_editing_ingredients.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox_editing_ingredientsItemStateChanged(evt);
            }
        });

        jLabel_editing_ingredient_name.setText("Name:");

        jTextField_editing_ingredient_name.setColumns(15);

        jLabel_editing_ingredient_type.setText("Food Type:");

        jComboBox_editing_ingredient_type.setModel(new DefaultComboBoxModel(FoodType.values())
        );

        jPanel_editing_ingredients_actions.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jButton_editing_ingredients_add.setText("ADD");
        jButton_editing_ingredients_add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_editing_ingredients_addActionPerformed(evt);
            }
        });

        jButton_editing_ingredients_remove.setText("REMOVE");
        jButton_editing_ingredients_remove.setEnabled(false);
        jButton_editing_ingredients_remove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_editing_ingredients_removeActionPerformed(evt);
            }
        });

        jButton_editing_ingredients_update.setText("UPDATE");
        jButton_editing_ingredients_update.setEnabled(false);
        jButton_editing_ingredients_update.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_editing_ingredients_updateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_editing_ingredients_actionsLayout = new javax.swing.GroupLayout(jPanel_editing_ingredients_actions);
        jPanel_editing_ingredients_actions.setLayout(jPanel_editing_ingredients_actionsLayout);
        jPanel_editing_ingredients_actionsLayout.setHorizontalGroup(
            jPanel_editing_ingredients_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_ingredients_actionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_editing_ingredients_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addGroup(jPanel_editing_ingredients_actionsLayout.createSequentialGroup()
                        .addComponent(jButton_editing_ingredients_update, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton_editing_ingredients_remove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jButton_editing_ingredients_add, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel_editing_ingredients_actionsLayout.setVerticalGroup(
            jPanel_editing_ingredients_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_ingredients_actionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton_editing_ingredients_add)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_ingredients_actionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jButton_editing_ingredients_update)
                    .addComponent(jButton_editing_ingredients_remove))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel_editing_ingredientsLayout = new javax.swing.GroupLayout(jPanel_editing_ingredients);
        jPanel_editing_ingredients.setLayout(jPanel_editing_ingredientsLayout);
        jPanel_editing_ingredientsLayout.setHorizontalGroup(
            jPanel_editing_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_ingredientsLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel_editing_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel_editing_ingredient, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel_editing_ingredient_name, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel_editing_ingredient_type, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel_editing_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jComboBox_editing_ingredients, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField_editing_ingredient_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox_editing_ingredient_type, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel_editing_ingredients_actions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel_editing_ingredientsLayout.setVerticalGroup(
            jPanel_editing_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_editing_ingredientsLayout.createSequentialGroup()
                .addGroup(jPanel_editing_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel_editing_ingredientsLayout.createSequentialGroup()
                        .addGroup(jPanel_editing_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(jLabel_editing_ingredient)
                            .addComponent(jComboBox_editing_ingredients, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel_editing_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel_editing_ingredient_name)
                            .addComponent(jTextField_editing_ingredient_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel_editing_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel_editing_ingredient_type)
                            .addComponent(jComboBox_editing_ingredient_type, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jPanel_editing_ingredients_actions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel_available_ingredients.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "AVAILABLE INGREDIENTS", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jTextArea_available_ingredients.setEditable(false);
        jTextArea_available_ingredients.setColumns(30);
        jTextArea_available_ingredients.setRows(20);
        jScrollPane_current_ingredients3.setViewportView(jTextArea_available_ingredients);

        javax.swing.GroupLayout jPanel_available_ingredientsLayout = new javax.swing.GroupLayout(jPanel_available_ingredients);
        jPanel_available_ingredients.setLayout(jPanel_available_ingredientsLayout);
        jPanel_available_ingredientsLayout.setHorizontalGroup(
            jPanel_available_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_available_ingredientsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane_current_ingredients3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_available_ingredientsLayout.setVerticalGroup(
            jPanel_available_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_available_ingredientsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane_current_ingredients3)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel_ingredientsLayout = new javax.swing.GroupLayout(jPanel_ingredients);
        jPanel_ingredients.setLayout(jPanel_ingredientsLayout);
        jPanel_ingredientsLayout.setHorizontalGroup(
            jPanel_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ingredientsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanel_editing_ingredients, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel_available_ingredients, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_ingredientsLayout.setVerticalGroup(
            jPanel_ingredientsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_ingredientsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_editing_ingredients, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel_available_ingredients, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane_tabs.addTab("Ingredients", jPanel_ingredients);

        javax.swing.GroupLayout jPanel_topLayout = new javax.swing.GroupLayout(jPanel_top);
        jPanel_top.setLayout(jPanel_topLayout);
        jPanel_topLayout.setHorizontalGroup(
            jPanel_topLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_topLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane_tabs)
                .addContainerGap())
        );
        jPanel_topLayout.setVerticalGroup(
            jPanel_topLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel_topLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane_tabs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel_top, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel_top, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton_modify_updateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_modify_updateActionPerformed
        update_day(jComboBox_modify_day.getSelectedIndex());        
    }//GEN-LAST:event_jButton_modify_updateActionPerformed

    private void jComboBox_modify_dayItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox_modify_dayItemStateChanged
        populate_choices(jComboBox_modify_day.getSelectedIndex());
    }//GEN-LAST:event_jComboBox_modify_dayItemStateChanged

    private void jButton_export_shopping_listActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_export_shopping_listActionPerformed
        try {
            FileWriter file_writer = new FileWriter(new File("Shopping List.rtf"));
            for(String line : shopping_list) {
                file_writer.write(line);                
            }
            file_writer.close();
        } catch (IOException ex) {
            System.out.println("I/O exception write error");
        }
    }//GEN-LAST:event_jButton_export_shopping_listActionPerformed

    private void jComboBox_editing_ingredientsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox_editing_ingredientsItemStateChanged
                
        if(jComboBox_editing_ingredients.getSelectedIndex() == 0) {
            jButton_editing_ingredients_add.setEnabled(true);
            jButton_editing_ingredients_update.setEnabled(false);
            jButton_editing_ingredients_remove.setEnabled(false);
            jTextField_editing_ingredient_name.setText("");
            jComboBox_editing_ingredient_type.setSelectedIndex(0);
        } else {
            jButton_editing_ingredients_add.setEnabled(false);
            jButton_editing_ingredients_update.setEnabled(true);
            jButton_editing_ingredients_remove.setEnabled(true);
            populate_editing_ingredients_info((String) jComboBox_editing_ingredients.getSelectedItem());
        }
    }//GEN-LAST:event_jComboBox_editing_ingredientsItemStateChanged

    private void jButton_editing_ingredients_addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_editing_ingredients_addActionPerformed
        String name = jTextField_editing_ingredient_name.getText();
        FoodType food_type = (FoodType) jComboBox_editing_ingredient_type.getSelectedItem();
        if(!name.isEmpty()) {
            if(!ingredients.containsKey(name)) {
                jButton_editing_ingredients_add.setEnabled(false);
                ingredients.put(
                    name,
                    new Ingredient(name, food_type)
                );
                try {
                    statement.executeUpdate(
                        "INSERT INTO ingredients (NAME, FOOD_TYPE) " +
                            "VALUES ('" +
                                name + "', '" +
                                food_type.toString() + "');"
                    );
                    System.out.println("Ingredient '" + name + "' added to database");
                    populate_available_ingredients();
                    jTextField_editing_ingredient_name.setText("");
                    jComboBox_editing_ingredient_type.setSelectedIndex(0);
                    jButton_editing_ingredients_add.setEnabled(true);
                } catch (SQLException ex) {
                    jButton_editing_ingredients_add.setEnabled(true);
                }                
            }            
        }
    }//GEN-LAST:event_jButton_editing_ingredients_addActionPerformed

    private void jButton_editing_ingredients_updateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_editing_ingredients_updateActionPerformed
        String new_name = jTextField_editing_ingredient_name.getText();
        FoodType food_type = (FoodType) jComboBox_editing_ingredient_type.getSelectedItem();
        String old_name = jComboBox_editing_ingredients.getSelectedItem().toString();
        if(!new_name.isEmpty()) {
            if(ingredients.containsKey(old_name)) {
                jButton_editing_ingredients_update.setEnabled(false);
                jButton_editing_ingredients_remove.setEnabled(false);
                ingredients.remove(old_name);
                ingredients.put(
                    new_name,
                    new Ingredient(new_name, food_type)
                );
                try {
                    statement.executeUpdate(                        
                        "UPDATE ingredients " +
                            "SET " +
                                "NAME='" + new_name + "', " +
                                "FOOD_TYPE='" + food_type.toString() + "' " +
                            "WHERE NAME='" + old_name + "';"
                    );
                    System.out.println("Ingredient '" + new_name + "' updated in ingredients table");
                    populate_available_ingredients();
                    update_main_courses_ingredients(old_name, new_name);
                    update_side_dishes_ingredients(old_name, new_name);
                    create_shopping_list();
                    jTextField_editing_ingredient_name.setText("");
                    jComboBox_editing_ingredient_type.setSelectedIndex(0);
                    jButton_editing_ingredients_add.setEnabled(true);
                } catch (SQLException ex) {
                    jButton_editing_ingredients_update.setEnabled(true);
                    jButton_editing_ingredients_remove.setEnabled(true);
                }
            }                
        }
    }//GEN-LAST:event_jButton_editing_ingredients_updateActionPerformed

    private void jButton_editing_ingredients_removeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_editing_ingredients_removeActionPerformed
        String name = jComboBox_editing_ingredients.getSelectedItem().toString();
        if(ingredients.containsKey(name)) {
            jButton_editing_ingredients_update.setEnabled(false);
            jButton_editing_ingredients_remove.setEnabled(false);
            ingredients.remove(name);
            try {
                statement.executeUpdate(
                    "DELETE FROM ingredients " +
                        "WHERE NAME='" + name + "';"
                );
                System.out.println("Ingredient '" + name + "' removed from ingredients table");
                populate_available_ingredients();
                update_main_courses_ingredients(name);
                update_side_dishes_ingredients(name);
                create_shopping_list();
                jTextField_editing_ingredient_name.setText("");
                jComboBox_editing_ingredient_type.setSelectedIndex(0);
                jButton_editing_ingredients_add.setEnabled(true);
            } catch (SQLException ex) {
                jButton_editing_ingredients_update.setEnabled(true);
                jButton_editing_ingredients_remove.setEnabled(true);
            }
        }
    }//GEN-LAST:event_jButton_editing_ingredients_removeActionPerformed

    private void jButton_editing_side_dish_addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_editing_side_dish_addActionPerformed
        String name = jTextField_editing_side_dish_name.getText();
        if(!name.isEmpty()) {
            if(!side_dishes.containsKey(name)) {
                jButton_editing_side_dish_add.setEnabled(false);                
                ArrayList<Ingredient> temp_ingredients = new ArrayList<Ingredient>();
                for(int i = 0; i < MAX_INGREDIENTS; i++) {
                    if(jComboBox_editing_side_dish_ingredient[i].getSelectedIndex() != 0) {
                        temp_ingredients.add(ingredients.get(jComboBox_editing_side_dish_ingredient[i].getSelectedItem().toString()));
                    }
                }
                side_dishes.put(name,
                                new FoodItem(name,
                                             SIDE_DISH,
                                             temp_ingredients
                                )
                );
                String sql = "INSERT INTO side_dishes (NAME";
                for(int i = 0; i < temp_ingredients.size(); i++) {
                    sql += (", INGREDIENT" + i);
                }
                sql += (") VALUES('" + name);
                for(int i = 0; i < temp_ingredients.size(); i++) {
                    sql += ("', '" + temp_ingredients.get(i).getName());
                }
                sql += "');";
                try {
                    statement.executeUpdate(sql);
                    System.out.println("Side Dish '" + name + "' added to database");
                    populate_available_side_dishes();
                    jTextField_editing_side_dish_name.setText("");
                    for(int i = 0; i < MAX_INGREDIENTS; i++) {
                        jComboBox_editing_side_dish_ingredient[i].setSelectedIndex(0);
                    }
                    jButton_editing_side_dish_add.setEnabled(true);
                } catch (SQLException ex) {
                    jButton_editing_side_dish_add.setEnabled(true);
                }                
            }            
        }
    }//GEN-LAST:event_jButton_editing_side_dish_addActionPerformed

    private void jButton_editing_side_dish_removeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_editing_side_dish_removeActionPerformed
        String name = jComboBox_editing_side_dishes.getSelectedItem().toString();
        if(side_dishes.containsKey(name)) {
            jButton_editing_side_dish_update.setEnabled(false);
            jButton_editing_side_dish_remove.setEnabled(false);
            side_dishes.remove(name);
            try {
                statement.executeUpdate(
                    "DELETE FROM side_dishes " +
                        "WHERE NAME = '" + name + "';"
                );
                System.out.println("Side Dish '" + name + "' removed from side_dishes table");
                populate_available_side_dishes();
                remove_side_dish_from_menu(name);
                create_shopping_list();
                jTextField_editing_side_dish_name.setText("");
                jComboBox_editing_side_dishes.setSelectedIndex(0);
                for(int i = 0; i < MAX_INGREDIENTS; i++) {
                    jComboBox_editing_side_dish_ingredient[i].setSelectedIndex(0);
                }
                jButton_editing_side_dish_add.setEnabled(true);
            } catch (SQLException ex) {
                jButton_editing_side_dish_update.setEnabled(true);
                jButton_editing_side_dish_remove.setEnabled(true);
            }
        }
    }//GEN-LAST:event_jButton_editing_side_dish_removeActionPerformed

    private void jButton_editing_side_dish_updateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_editing_side_dish_updateActionPerformed
        String original_name = jComboBox_editing_side_dishes.getSelectedItem().toString();
        String name = jTextField_editing_side_dish_name.getText();
        if(!name.isEmpty()) {
            if(name.matches(original_name) || !side_dishes.containsKey(name)) {
                jButton_editing_side_dish_update.setEnabled(false);
                jButton_editing_side_dish_remove.setEnabled(false);
                ArrayList<Ingredient> temp_ingredients = new ArrayList<Ingredient>();
                for(int i = 0; i < MAX_INGREDIENTS; i++) {
                    if(jComboBox_editing_side_dish_ingredient[i].getSelectedIndex() != 0) {
                        temp_ingredients.add(ingredients.get(jComboBox_editing_side_dish_ingredient[i].getSelectedItem().toString()));
                    }
                }
                side_dishes.remove(original_name);
                side_dishes.put(name,
                                new FoodItem(name,
                                             SIDE_DISH,
                                             temp_ingredients
                                )
                );
                String sql;
                if(!name.matches(original_name)) {
                    sql = ("UPDATE side_dishes SET NAME='" + name + "', ");
                } else {
                    sql = ("UPDATE side_dishes SET ");
                }
                for(int i = 0, max = temp_ingredients.size(); i < MAX_INGREDIENTS; i++) {
                    if(i < max) {
                        sql += ("INGREDIENT" + i + "='" + temp_ingredients.get(i).getName()) + "', ";
                    } else {
                        sql += ("INGREDIENT" + i + "=null");
                        if(i < (MAX_INGREDIENTS - 1)) {
                            sql += ", ";
                        }
                    }
                }
                sql += (" WHERE NAME='" + original_name + "';");
                try {
                    statement.executeUpdate(sql);
                    System.out.println("Side Dish '" + name + "(" + original_name + "')" + "' updated in database");
                    populate_available_side_dishes();
                    for(int i = 0; i < DAYS; i++) {
                        if(side_dish1[i].matches(original_name)) {
                            side_dish1[i] = name;
                        }
                        if(side_dish2[i].matches(original_name)) {
                            side_dish2[i] = name;
                        }
                        update_menu(i);
                    }
                    populate_choices(jComboBox_modify_day.getSelectedIndex());
                    create_shopping_list();
                    jTextField_editing_side_dish_name.setText("");
                    for(int i = 0; i < MAX_INGREDIENTS; i++) {
                        jComboBox_editing_side_dish_ingredient[i].setSelectedIndex(0);
                    }
                    jButton_editing_side_dish_add.setEnabled(true);
                } catch (SQLException ex) {
                    jButton_editing_side_dish_add.setEnabled(true);
                }                
            }            
        }
    }//GEN-LAST:event_jButton_editing_side_dish_updateActionPerformed

    private void jComboBox_editing_side_dishesItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox_editing_side_dishesItemStateChanged
        if(jComboBox_editing_side_dishes.getSelectedIndex() == 0) {
            jButton_editing_side_dish_add.setEnabled(true);
            jButton_editing_side_dish_update.setEnabled(false);
            jButton_editing_side_dish_remove.setEnabled(false);
            jTextField_editing_side_dish_name.setText("");
            for(int i = 0; i < MAX_INGREDIENTS; i++) {
                jComboBox_editing_side_dish_ingredient[i].setSelectedIndex(0);
            }
        } else {
            jButton_editing_side_dish_add.setEnabled(false);
            jButton_editing_side_dish_update.setEnabled(true);
            jButton_editing_side_dish_remove.setEnabled(true);
            populate_editing_side_dishes_info(jComboBox_editing_side_dishes.getSelectedItem().toString());
        }
    }//GEN-LAST:event_jComboBox_editing_side_dishesItemStateChanged

    private void jButton_editing_main_course_addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_editing_main_course_addActionPerformed
        String name = jTextField_editing_main_course_name.getText();
        if(!name.isEmpty()) {
            if(!main_courses.containsKey(name)) {
                jButton_editing_main_course_add.setEnabled(false);                
                ArrayList<Ingredient> temp_ingredients = new ArrayList<Ingredient>();
                for(int i = 0; i < MAX_INGREDIENTS; i++) {
                    if(jComboBox_editing_main_course_ingredient[i].getSelectedIndex() != 0) {
                        temp_ingredients.add(ingredients.get(jComboBox_editing_main_course_ingredient[i].getSelectedItem().toString()));
                    }
                }
                main_courses.put(name,
                                new FoodItem(name,
                                             MAIN_COURSE,
                                             temp_ingredients
                                )
                );
                String sql = "INSERT INTO main_courses (NAME";
                for(int i = 0; i < temp_ingredients.size(); i++) {
                    sql += (", INGREDIENT" + i);
                }
                sql += (") VALUES('" + name);
                for(int i = 0; i < temp_ingredients.size(); i++) {
                    sql += ("', '" + temp_ingredients.get(i).getName());
                }
                sql += "');";
                try {
                    statement.executeUpdate(sql);
                    System.out.println("Main Course '" + name + "' added to database");
                    populate_available_main_courses();
                    jTextField_editing_main_course_name.setText("");
                    for(int i = 0; i < MAX_INGREDIENTS; i++) {
                        jComboBox_editing_main_course_ingredient[i].setSelectedIndex(0);
                    }
                    jButton_editing_main_course_add.setEnabled(true);
                } catch (SQLException ex) {
                    jButton_editing_main_course_add.setEnabled(true);
                }                
            }            
        }
    }//GEN-LAST:event_jButton_editing_main_course_addActionPerformed

    private void jButton_editing_main_course_removeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_editing_main_course_removeActionPerformed
        String name = jComboBox_editing_main_courses.getSelectedItem().toString();
        if(main_courses.containsKey(name)) {
            jButton_editing_main_course_update.setEnabled(false);
            jButton_editing_main_course_remove.setEnabled(false);
            main_courses.remove(name);
            try {
                statement.executeUpdate(
                    "DELETE FROM main_courses " +
                        "WHERE NAME = '" + name + "';"
                );
                System.out.println("Main Course '" + name + "' removed from main_courses table");
                populate_available_main_courses();
                remove_main_course_from_menu(name);
                create_shopping_list();
                jTextField_editing_main_course_name.setText("");
                jComboBox_editing_main_courses.setSelectedIndex(0);
                for(int i = 0; i < MAX_INGREDIENTS; i++) {
                    jComboBox_editing_main_course_ingredient[i].setSelectedIndex(0);
                }
                jButton_editing_main_course_add.setEnabled(true);
            } catch (SQLException ex) {
                jButton_editing_main_course_update.setEnabled(true);
                jButton_editing_main_course_remove.setEnabled(true);
            }
        }
    }//GEN-LAST:event_jButton_editing_main_course_removeActionPerformed

    private void jButton_editing_main_course_updateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_editing_main_course_updateActionPerformed
        String original_name = jComboBox_editing_main_courses.getSelectedItem().toString();
        String name = jTextField_editing_main_course_name.getText();
        if(!name.isEmpty()) {
            if(name.matches(original_name) || !main_courses.containsKey(name)) {
                jButton_editing_main_course_update.setEnabled(false);
                jButton_editing_main_course_remove.setEnabled(false);
                ArrayList<Ingredient> temp_ingredients = new ArrayList<Ingredient>();
                for(int i = 0; i < MAX_INGREDIENTS; i++) {
                    if(jComboBox_editing_main_course_ingredient[i].getSelectedIndex() != 0) {
                        temp_ingredients.add(ingredients.get(jComboBox_editing_main_course_ingredient[i].getSelectedItem().toString()));
                    }
                }
                main_courses.remove(original_name);
                main_courses.put(name,
                                new FoodItem(name,
                                             MAIN_COURSE,
                                             temp_ingredients
                                )
                );
                String sql;
                if(!name.matches(original_name)) {
                    sql = ("UPDATE main_courses SET NAME='" + name + "', ");
                } else {
                    sql = ("UPDATE main_courses SET ");
                }
                for(int i = 0, max = temp_ingredients.size(); i < MAX_INGREDIENTS; i++) {
                    if(i < max) {
                        sql += ("INGREDIENT" + i + "='" + temp_ingredients.get(i).getName()) + "', ";
                    } else {
                        sql += ("INGREDIENT" + i + "=null");
                        if(i < (MAX_INGREDIENTS - 1)) {
                            sql += ", ";
                        }
                    }
                }
                sql += (" WHERE NAME='" + original_name + "';");
                try {
                    statement.executeUpdate(sql);
                    if(!name.matches(original_name)) {
                        System.out.println("Main Course " + name + " (was " + original_name + ")" + " updated in database");
                    } else {
                        System.out.println("Main Course " + name + " updated in database");
                    }
                    populate_available_main_courses();
                    for(int i = 0; i < DAYS; i++) {
                        if(main_course[i].matches(original_name)) {
                            main_course[i] = name;
                        }
                        update_menu(i);
                    }
                    populate_choices(jComboBox_modify_day.getSelectedIndex());
                    create_shopping_list();
                    jTextField_editing_main_course_name.setText("");
                    for(int i = 0; i < MAX_INGREDIENTS; i++) {
                        jComboBox_editing_main_course_ingredient[i].setSelectedIndex(0);
                    }
                    jButton_editing_main_course_add.setEnabled(true);
                } catch (SQLException ex) {
                    jButton_editing_main_course_add.setEnabled(true);
                }                
            }            
        }
    }//GEN-LAST:event_jButton_editing_main_course_updateActionPerformed

    private void jComboBox_editing_main_coursesItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox_editing_main_coursesItemStateChanged
        if(jComboBox_editing_main_courses.getSelectedIndex() == 0) {
            jButton_editing_main_course_add.setEnabled(true);
            jButton_editing_main_course_update.setEnabled(false);
            jButton_editing_main_course_remove.setEnabled(false);
            jTextField_editing_main_course_name.setText("");
            for(int i = 0; i < MAX_INGREDIENTS; i++) {
                jComboBox_editing_main_course_ingredient[i].setSelectedIndex(0);
            }
        } else {
            jButton_editing_main_course_add.setEnabled(false);
            jButton_editing_main_course_update.setEnabled(true);
            jButton_editing_main_course_remove.setEnabled(true);
            populate_editing_main_courses_info(jComboBox_editing_main_courses.getSelectedItem().toString());
        }
    }//GEN-LAST:event_jComboBox_editing_main_coursesItemStateChanged

    private void jButton_export_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_export_menuActionPerformed
        try {
            FileWriter file_writer = new FileWriter(new File("Menu.rtf"));
            for(int i = 0; i < day.length; i++) {
                file_writer.write(day[i].toUpperCase() + newline);
                if(!main_course[i].contains("NONE")) {
                    file_writer.write("  " + main_course[i] + newline);
                }
                if(!side_dish1[i].contains("NONE")) {
                    file_writer.write("  " + side_dish1[i] + newline);
                }
                if(!side_dish2[i].contains("NONE")) {
                    file_writer.write("  " + side_dish2[i] + newline);
                }
                file_writer.write(newline);
            }
            file_writer.close();
        } catch (IOException ex) {
            System.out.println("I/O exception write error");
        }
    }//GEN-LAST:event_jButton_export_menuActionPerformed

    /**
     * @param args the command line arguments
     * @throws javax.swing.UnsupportedLookAndFeelException
     */
    public static void main(String args[]) throws UnsupportedLookAndFeelException {
        
        try {
            for(LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex1) {}
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GroceryListMaker().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton_editing_ingredients_add;
    private javax.swing.JButton jButton_editing_ingredients_remove;
    private javax.swing.JButton jButton_editing_ingredients_update;
    private javax.swing.JButton jButton_editing_main_course_add;
    private javax.swing.JButton jButton_editing_main_course_remove;
    private javax.swing.JButton jButton_editing_main_course_update;
    private javax.swing.JButton jButton_editing_side_dish_add;
    private javax.swing.JButton jButton_editing_side_dish_remove;
    private javax.swing.JButton jButton_editing_side_dish_update;
    private javax.swing.JButton jButton_export_menu;
    private javax.swing.JButton jButton_export_shopping_list;
    private javax.swing.JButton jButton_modify_update;
    private javax.swing.JComboBox<String> jComboBox_editing_ingredient_type;
    private javax.swing.JComboBox<String> jComboBox_editing_ingredients;
    private javax.swing.JComboBox<String>[] jComboBox_editing_main_course_ingredient;
    private javax.swing.JComboBox<String> jComboBox_editing_main_courses;
    private javax.swing.JComboBox<String>[] jComboBox_editing_side_dish_ingredient;
    private javax.swing.JComboBox<String> jComboBox_editing_side_dishes;
    private javax.swing.JComboBox jComboBox_modify_day;
    private javax.swing.JComboBox jComboBox_modify_main_course;
    private javax.swing.JComboBox jComboBox_modify_side1;
    private javax.swing.JComboBox jComboBox_modify_side2;
    private javax.swing.JLabel jLabel_editing_ingredient;
    private javax.swing.JLabel jLabel_editing_ingredient_name;
    private javax.swing.JLabel jLabel_editing_ingredient_type;
    private javax.swing.JLabel jLabel_editing_main_course;
    private javax.swing.JLabel jLabel_editing_main_course_ingredients;
    private javax.swing.JLabel jLabel_editing_main_course_name;
    private javax.swing.JLabel jLabel_editing_side_dish;
    private javax.swing.JLabel jLabel_editing_side_dish_ingredients;
    private javax.swing.JLabel jLabel_editing_side_dish_name;
    private javax.swing.JLabel jLabel_menu_friday;
    private javax.swing.JLabel jLabel_menu_monday;
    private javax.swing.JLabel jLabel_menu_saturday;
    private javax.swing.JLabel jLabel_menu_sunday;
    private javax.swing.JLabel jLabel_menu_thursday;
    private javax.swing.JLabel jLabel_menu_tuesday;
    private javax.swing.JLabel jLabel_menu_wednesday;
    private javax.swing.JLabel jLabel_modify_day;
    private javax.swing.JLabel jLabel_modify_main_course;
    private javax.swing.JLabel jLabel_modify_people;
    private javax.swing.JLabel jLabel_modify_side1;
    private javax.swing.JLabel jLabel_modify_side2;
    private javax.swing.JPanel jPanel_available_ingredients;
    private javax.swing.JPanel jPanel_available_main_courses;
    private javax.swing.JPanel jPanel_available_side_dishes;
    private javax.swing.JPanel jPanel_editing_ingredients;
    private javax.swing.JPanel jPanel_editing_ingredients_actions;
    private javax.swing.JPanel jPanel_editing_main_course_actions;
    private javax.swing.JPanel jPanel_editing_main_courses;
    private javax.swing.JPanel jPanel_editing_side_dish_actions;
    private javax.swing.JPanel jPanel_editing_side_dishes;
    private javax.swing.JPanel jPanel_ingredients;
    private javax.swing.JPanel jPanel_list;
    private javax.swing.JPanel jPanel_main;
    private javax.swing.JPanel jPanel_main_courses;
    private javax.swing.JPanel jPanel_menu;
    private javax.swing.JPanel jPanel_modify;
    private javax.swing.JPanel jPanel_side_dishes;
    private javax.swing.JPanel jPanel_top;
    private javax.swing.JScrollPane jScrollPane_current_ingredients1;
    private javax.swing.JScrollPane jScrollPane_current_ingredients2;
    private javax.swing.JScrollPane jScrollPane_current_ingredients3;
    private javax.swing.JScrollPane jScrollPane_list;
    private javax.swing.JScrollPane jScrollPane_menu_friday;
    private javax.swing.JScrollPane jScrollPane_menu_monday;
    private javax.swing.JScrollPane jScrollPane_menu_saturday;
    private javax.swing.JScrollPane jScrollPane_menu_sunday;
    private javax.swing.JScrollPane jScrollPane_menu_thursday;
    private javax.swing.JScrollPane jScrollPane_menu_tuesday;
    private javax.swing.JScrollPane jScrollPane_menu_wednesday;
    private javax.swing.JSpinner jSpinner_modify_people;
    private javax.swing.JTabbedPane jTabbedPane_tabs;
    private javax.swing.JTextArea jTextArea_available_ingredients;
    private javax.swing.JTextArea jTextArea_available_main_courses;
    private javax.swing.JTextArea jTextArea_available_side_dishes;
    private javax.swing.JTextArea jTextArea_list;
    private javax.swing.JTextArea jTextArea_menu_friday;
    private javax.swing.JTextArea jTextArea_menu_monday;
    private javax.swing.JTextArea jTextArea_menu_saturday;
    private javax.swing.JTextArea jTextArea_menu_sunday;
    private javax.swing.JTextArea jTextArea_menu_thursday;
    private javax.swing.JTextArea jTextArea_menu_tuesday;
    private javax.swing.JTextArea jTextArea_menu_wednesday;
    private javax.swing.JTextField jTextField_editing_ingredient_name;
    private javax.swing.JTextField jTextField_editing_main_course_name;
    private javax.swing.JTextField jTextField_editing_side_dish_name;
    // End of variables declaration//GEN-END:variables
}
