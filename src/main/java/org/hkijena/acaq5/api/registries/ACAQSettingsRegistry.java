package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;

import javax.swing.*;

/**
 * Registry for settings.
 * Settings are organized in "sheets" (parameter collections)
 */
public class ACAQSettingsRegistry {

//    private BiMap<String, >

    /**
     * Registers a new settings sheet
     * @param id unique ID of the sheet
     * @param name sheet name
     * @param category sheet category. If left null or empty, it will default to "General"
     * @param categoryIcon optional icon. If null, a wrench icon is used.
     * @param parameterCollection the object that holds the parameters
     */
    public void register(String id, String name, String category, Icon categoryIcon, ACAQParameterCollection parameterCollection) {
        Sheet sheet = new Sheet(name, category, categoryIcon, parameterCollection);
    }

    /**
     * A settings sheet
     */
    public static class Sheet {
        private String name;
        private String category;
        private Icon categoryIcon;
        private ACAQParameterCollection parameterCollection;

        /**
         * Creates a new instance
         * @param name name shown in UI
         * @param category category shown in UI
         * @param categoryIcon category icon
         * @param parameterCollection object that holds the parameter
         */
        public Sheet(String name, String category, Icon categoryIcon, ACAQParameterCollection parameterCollection) {
            this.name = name;
            this.category = category;
            this.categoryIcon = categoryIcon;
            this.parameterCollection = parameterCollection;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public Icon getCategoryIcon() {
            return categoryIcon;
        }

        public ACAQParameterCollection getParameterCollection() {
            return parameterCollection;
        }
    }

}
