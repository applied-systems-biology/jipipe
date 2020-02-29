/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.registries;


import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableToTableOperation;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableToTableOperationUI;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableVectorOperation;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableVectorOperationUI;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ACAQTableAnalyzerUIOperationRegistry {
    private Map<Class<? extends ACAQTableVectorOperation>, VectorOperationEntry> vectorOperationEntries = new HashMap<>();
    private Map<Class<? extends ACAQTableToTableOperation>, TableToTableOperationEntry> tableToTableOperationEntries = new HashMap<>();

    public void register(Class<? extends ACAQTableVectorOperation> operationClass,
                         Class<? extends ACAQTableVectorOperationUI> uiClass,
                         String name,
                         String shortcut,
                         String description,
                         Icon icon) {
        vectorOperationEntries.put(operationClass, new VectorOperationEntry(operationClass,
                uiClass,
                name,
                shortcut,
                description,
                icon));
    }

    public ACAQTableVectorOperationUI createUIForVectorOperation(ACAQTableVectorOperation operation) {
        try {
            if (vectorOperationEntries.get(operation.getClass()).getUiClass() == null)
                return null;
            return vectorOperationEntries.get(operation.getClass()).getUiClass().getConstructor(ACAQTableVectorOperation.class).newInstance(operation);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public ACAQTableToTableOperationUI createUIForTableToTableOperation(ACAQTableToTableOperation operation) {
        try {
            if (tableToTableOperationEntries.get(operation.getClass()).getUiClass() == null)
                return null;
            return tableToTableOperationEntries.get(operation.getClass()).getUiClass().getConstructor(ACAQTableToTableOperation.class).newInstance(operation);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<VectorOperationEntry> getVectorOperationEntries() {
        return vectorOperationEntries.values();
    }

    public Collection<TableToTableOperationEntry> getTableToTableOperationEntries() {
        return tableToTableOperationEntries.values();
    }

    public String getNameOf(ACAQTableVectorOperation operation) {
        return vectorOperationEntries.get(operation.getClass()).getName();
    }

    public String getShortcutOf(ACAQTableVectorOperation operation) {
        return vectorOperationEntries.get(operation.getClass()).getShortcut();
    }

    public Icon getIconOf(ACAQTableVectorOperation operation) {
        return vectorOperationEntries.get(operation.getClass()).getIcon();
    }

    public String getNameOf(ACAQTableToTableOperation operation) {
        return tableToTableOperationEntries.get(operation.getClass()).getName();
    }

    public Icon getIconOf(ACAQTableToTableOperation operation) {
        return tableToTableOperationEntries.get(operation.getClass()).getIcon();
    }

    public static class VectorOperationEntry {
        private Class<? extends ACAQTableVectorOperation> operationClass;
        private Class<? extends ACAQTableVectorOperationUI> uiClass;
        private String name;
        private String shortcut;
        private String description;
        private Icon icon;

        public VectorOperationEntry(Class<? extends ACAQTableVectorOperation> operationClass, Class<? extends ACAQTableVectorOperationUI> uiClass, String name, String shortcut, String description, Icon icon) {
            this.operationClass = operationClass;
            this.uiClass = uiClass;
            this.name = name;
            this.shortcut = shortcut;
            this.description = description;
            this.icon = icon;
        }

        public Class<? extends ACAQTableVectorOperationUI> getUiClass() {
            return uiClass;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Icon getIcon() {
            return icon;
        }

        public Class<? extends ACAQTableVectorOperation> getOperationClass() {
            return operationClass;
        }

        public ACAQTableVectorOperation instantiateOperation() {
            try {
                return operationClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public String getShortcut() {
            return shortcut;
        }
    }

    public static class TableToTableOperationEntry {
        private Class<? extends ACAQTableToTableOperation> operationClass;
        private Class<? extends ACAQTableToTableOperationUI> uiClass;
        private String name;
        private String description;
        private Icon icon;

        public TableToTableOperationEntry(Class<? extends ACAQTableToTableOperation> operationClass, Class<? extends ACAQTableToTableOperationUI> uiClass, String name, String description, Icon icon) {
            this.operationClass = operationClass;
            this.uiClass = uiClass;
            this.name = name;
            this.description = description;
            this.icon = icon;
        }

        public Class<? extends ACAQTableToTableOperationUI> getUiClass() {
            return uiClass;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Icon getIcon() {
            return icon;
        }

        public Class<? extends ACAQTableToTableOperation> getOperationClass() {
            return operationClass;
        }

        public ACAQTableToTableOperation instantiateOperation() {
            try {
                return operationClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
