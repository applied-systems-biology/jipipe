package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterHolder;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.api.parameters.CollectionParameter;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.annotation.Annotation;

public class CollectionParameterEditorUI extends ACAQParameterEditorUI {

    private FormPanel listPanel;

    public CollectionParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        listPanel = new FormPanel(null, false, false, false);
        add(listPanel, BorderLayout.CENTER);
    }

    private void addEntry() {
        CollectionParameter collectionParameter = getParameterAccess().get();
        collectionParameter.add(null);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        listPanel.clear();
        CollectionParameter collectionParameter = getParameterAccess().get();
        for (int i = 0; i < collectionParameter.size(); ++i) {
            ACAQParameterAccess entryAccess = new EntryAccess(getParameterAccess(), collectionParameter, i);
            JButton removeButton = new JButton(UIUtils.getIconFromResources("delete.png"));
            UIUtils.makeFlat25x25(removeButton);
            removeButton.setToolTipText("Remove entry");
            int index = i;
            removeButton.addActionListener(e -> collectionParameter.remove(index));
            listPanel.addToForm(ACAQUIParametertypeRegistry.getInstance().createEditorFor(getWorkbenchUI(), entryAccess), null);
        }

        JButton addEntryButton = new JButton(UIUtils.getIconFromResources("add.png"));
        addEntryButton.setToolTipText("Add entry");
        addEntryButton.addActionListener(e -> addEntry());
        UIUtils.makeFlat(addEntryButton);
        listPanel.addToForm(addEntryButton, null);
    }

    /**
     * Access to one entry
     */
    public static class EntryAccess implements ACAQParameterAccess {

        private ACAQParameterAccess parentAccess;
        private CollectionParameter collectionParameter;
        private int index;

        public EntryAccess(ACAQParameterAccess parentAccess, CollectionParameter<?> collectionParameter, int index) {
            this.parentAccess = parentAccess;
            this.collectionParameter = collectionParameter;
            this.index = index;
        }

        @Override
        public String getKey() {
            return parentAccess.getKey() + "/" + index;
        }

        @Override
        public String getName() {
            return "Item " + (index + 1);
        }

        @Override
        public String getDescription() {
            return parentAccess.getDescription();
        }

        @Override
        public ACAQParameterVisibility getVisibility() {
            return parentAccess.getVisibility();
        }

        @Override
        public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
            return parentAccess.getAnnotationOfType(klass);
        }

        @Override
        public Class<?> getFieldClass() {
            return collectionParameter.getContentClass();
        }

        @Override
        public <T> T get() {
            return (T) collectionParameter.get(index);
        }

        @Override
        public <T> boolean set(T value) {
            collectionParameter.set(index, value);
            return true;
        }

        @Override
        public ACAQParameterHolder getParameterHolder() {
            return parentAccess.getParameterHolder();
        }

        @Override
        public String getHolderName() {
            return parentAccess.getHolderName();
        }

        @Override
        public String getHolderDescription() {
            return parentAccess.getHolderDescription();
        }
    }
}
