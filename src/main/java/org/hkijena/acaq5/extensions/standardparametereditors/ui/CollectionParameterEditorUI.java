package org.hkijena.acaq5.extensions.standardparametereditors.ui;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.CollectionEntryParameterAccess;
import org.hkijena.acaq5.api.parameters.CollectionParameter;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Generic parameter for {@link org.hkijena.acaq5.api.parameters.CollectionParameter}
 */
public class CollectionParameterEditorUI extends ACAQParameterEditorUI {
    private FormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public CollectionParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());

        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("add.png"));
        addButton.addActionListener(e -> addNewEntry());
        toolBar.add(addButton);
        add(toolBar, BorderLayout.NORTH);

        formPanel = new FormPanel(null, false, false, false);
        add(formPanel, BorderLayout.CENTER);
    }

    private void addNewEntry() {
        getParameter().addNewInstance();
        reload();
    }

    private CollectionParameter<?> getParameter() {
        return getParameterAccess().get();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        formPanel.clear();
        for (int i = 0; i < getParameter().size(); ++i) {
            Object entry = getParameter().get(i);
            JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
            removeButton.setToolTipText("Remove entry");
            UIUtils.makeBorderlessWithoutMargin(removeButton);
            removeButton.addActionListener(e -> removeEntry(entry));

            CollectionEntryParameterAccess<?> access = new CollectionEntryParameterAccess(getParameterAccess(),
                    getParameter(),
                    getParameter().getContentClass(),
                    i);
            ACAQParameterEditorUI ui = ACAQUIParametertypeRegistry.getInstance().createEditorFor(getContext(), access);
            formPanel.addToForm(ui, removeButton, null);
        }
    }

    private void removeEntry(Object entry) {
        getParameter().remove(entry);
        reload();
    }
}
