/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.grouping.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.FancyTextField;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * UI for {@link GraphNodeParameterReferenceGroup}
 */
public class GraphNodeParameterReferenceGroupUI extends JPanel {
    private final GraphNodeParametersUI parametersUI;
    private final GraphNodeParameterReferenceGroup group;
    private FormPanel contentPanel;

    /**
     * @param parametersUI the parent
     * @param group        the group
     */
    public GraphNodeParameterReferenceGroupUI(GraphNodeParametersUI parametersUI, GraphNodeParameterReferenceGroup group) {
        super(new BorderLayout());
        this.parametersUI = parametersUI;
        this.group = group;
        this.initialize();
        refreshUI();
    }

    private void initialize() {
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));
        GroupMetadataEditor groupMetadataEditor = new GroupMetadataEditor(this, group);
        groupMetadataEditor.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        add(groupMetadataEditor, BorderLayout.NORTH);

        contentPanel = new FormPanel(null, FormPanel.NONE);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void refreshUI() {
        contentPanel.clear();
        for (GraphNodeParameterReference reference : group.getContent()) {
            JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
            UIUtils.makeBorderlessWithoutMargin(removeButton);
            removeButton.addActionListener(e -> group.removeContent(reference));

            contentPanel.addToForm(new GraphNodeParameterReferenceUI(this, reference), removeButton, null);
        }
        JButton addButton = new JButton("Add parameter", UIUtils.getIconFromResources("actions/list-add.png"));
        UIUtils.makeFlat(addButton);
        addButton.setBorder(BorderFactory.createDashedBorder(Color.GRAY));
        addButton.addActionListener(e -> addReference());
        contentPanel.addWideToForm(addButton, null);
    }

    private void addReference() {
        JIPipeParameterTree tree = parametersUI.getTree();
        List<Object> selected = ParameterTreeUI.showPickerDialog(this, tree, "Add parameter");
        List<GraphNodeParameterReference> referenceList = new ArrayList<>();
        for (Object parameter : selected) {
            if (parameter != null) {
                for (JIPipeParameterAccess child : tree.getAllChildParameters(parameter)) {
                    referenceList.add(new GraphNodeParameterReference(child, tree));
                }
            }
        }
        group.addContent(referenceList);
    }

    public GraphNodeParametersUI getParametersUI() {
        return parametersUI;
    }

    /**
     * Edits name and description of a {@link GraphNodeParameterReferenceGroup}
     */
    public static class GroupMetadataEditor extends JPanel {
        private final GraphNodeParameterReferenceGroup group;

        /**
         * @param parent the parent
         * @param group  the group to be edited
         */
        public GroupMetadataEditor(GraphNodeParameterReferenceGroupUI parent, GraphNodeParameterReferenceGroup group) {
            this.group = group;
            initialize();
        }

        private void initialize() {
            setLayout(new BorderLayout());

            FancyTextField nameEditor = new FancyTextField(new JLabel(UIUtils.getIconFromResources("actions/configure.png")),
                    "Group name");
            nameEditor.setText(group.getName());
            nameEditor.getTextField().getDocument().addDocumentListener(new DocumentChangeListener() {
                @Override
                public void changed(DocumentEvent documentEvent) {
                    group.setName(nameEditor.getText());
                }
            });
            add(nameEditor, BorderLayout.CENTER);

            JButton changeDescriptionButton = new JButton("Edit description", UIUtils.getIconFromResources("actions/edit-select-text.png"));
            UIUtils.makeFlat(changeDescriptionButton);
            changeDescriptionButton.setToolTipText("Change description");
            changeDescriptionButton.addActionListener(e -> changeDescription());
            add(changeDescriptionButton, BorderLayout.EAST);
        }

        private void changeDescription() {
            HTMLText currentDescription = group.getDescription();
            HTMLText newDescription = UIUtils.getHTMLByDialog(this, "Set description", "Please enter a new description:", currentDescription);
            if (newDescription != null) {
                group.setDescription(newDescription);
            }
        }
    }
}
