package org.hkijena.acaq5.api.grouping.parameters;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.components.FancyTextField;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.ParameterTreeUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
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
            JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
            UIUtils.makeBorderlessWithoutMargin(removeButton);
            removeButton.addActionListener(e -> group.removeContent(reference));

            contentPanel.addToForm(new GraphNodeParameterReferenceUI(this, reference), removeButton, null);
        }
        JButton addButton = new JButton("Add parameter", UIUtils.getIconFromResources("add.png"));
        UIUtils.makeFlat(addButton);
        addButton.setBorder(BorderFactory.createDashedBorder(Color.GRAY));
        addButton.addActionListener(e -> addReference());
        contentPanel.addWideToForm(addButton, null);
    }

    private void addReference() {
        ACAQParameterTree tree = parametersUI.getTree();
        List<Object> selected = ParameterTreeUI.showPickerDialog(this, tree, "Add parameter");
        List<GraphNodeParameterReference> referenceList = new ArrayList<>();
        for (Object parameter : selected) {
            if (parameter != null) {
                for (ACAQParameterAccess child : tree.getAllChildParameters(parameter)) {
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

            FancyTextField nameEditor = new FancyTextField(new JLabel(UIUtils.getIconFromResources("cog.png")),
                    "Group name");
            nameEditor.setText(group.getName());
            nameEditor.getTextField().getDocument().addDocumentListener(new DocumentChangeListener() {
                @Override
                public void changed(DocumentEvent documentEvent) {
                    group.setName(nameEditor.getText());
                }
            });
            add(nameEditor, BorderLayout.CENTER);

            JButton changeDescriptionButton = new JButton("Edit description", UIUtils.getIconFromResources("text2.png"));
            UIUtils.makeFlat(changeDescriptionButton);
            changeDescriptionButton.setToolTipText("Change description");
            changeDescriptionButton.addActionListener(e -> changeDescription());
            add(changeDescriptionButton, BorderLayout.EAST);
        }

        private void changeDescription() {
            String currentDescription = group.getDescription();
            String newDescription = UIUtils.getMultiLineStringByDialog(this, "Set description", "Please enter a new description:", currentDescription);
            if (newDescription != null) {
                group.setDescription(newDescription);
            }
        }
    }
}
