/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.graphannotation.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopFormGraphEditorToolPanel;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPalette;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPaletteColor;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPaletteUI;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopRegistryBackedColorPaletteUserColorStorage;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.graphannotation.nodes.TextBoxAnnotationGraphNode;
import org.hkijena.jipipe.plugins.graphannotation.nodes.TextBoxAnnotationGraphNode;
import org.hkijena.jipipe.plugins.nodetemplate.JIPipeNodeTemplatePickerDialog;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

public class TextBoxAnnotationGraphNodeToolProperties extends JIPipeDesktopFormGraphEditorToolPanel<TextBoxAnnotationGraphNodeTool> implements JIPipeDesktopColorPaletteUI.SelectedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {

    private static JIPipeDesktopColorPaletteColor LAST_COLOR = JIPipeDesktopColorPalette.PASTEL[0];
    private static Anchor LAST_ANCHOR = Anchor.CenterCenter;
    private static List<JIPipeNodeTemplate> PRESETS;
    private static JIPipeNodeTemplate LAST_PRESET;
    private static final String PRESET_DOCUMENTATION = """
            # Presets
            
            Create presets to create designs for newly created annotations.
            Please note that the border/background color and text location are determined by the other controls.
            """;

    private final JIPipeDesktopColorPaletteUI paletteUI;
    private final JIPipeDesktopParameterEditorUI<Anchor> anchorParameterEditorUI;
    private final JButton pickPresetButton;
    private final JButton editPresetButton;

    public TextBoxAnnotationGraphNodeToolProperties(JIPipeDesktopGraphEditorUI graphEditorUI, TextBoxAnnotationGraphNodeTool tool) {
        super(graphEditorUI, tool);

        // Preset picker
        initializePreset();
        this.pickPresetButton = UIUtils.createButton("PRESET", JIPipe.RESOURCES.getIcon16("actions/template.png"), this::pickPreset);
        this.editPresetButton = UIUtils.createIconOnlyButton("Show preset options", JIPipe.RESOURCES.getIcon16("actions/configure.png"), () -> {});
        JPopupMenu editMenu = UIUtils.addPopupMenuToButton(editPresetButton);
        editMenu.add(UIUtils.createMenuItem("Edit", "Edits the current preset", JIPipe.RESOURCES.getIcon16("actions/edit.png"), this::editPreset));
        editMenu.add(UIUtils.createMenuItem("Rename", "Rename the current preset", JIPipe.RESOURCES.getIcon16("actions/label.png"), this::renamePreset));
        editMenu.add(UIUtils.createMenuItem("Remove", "Deletes the current preset", JIPipe.RESOURCES.getIcon16("actions/entry-delete.png"), this::deletePreset));
        editMenu.addSeparator();
        editMenu.add(UIUtils.createMenuItem("Add", "Adds a new preset as copy of the selected one", JIPipe.RESOURCES.getIcon16("actions/list-add.png"), this::addPreset));

        // Initialize palette
        this.paletteUI = new JIPipeDesktopColorPaletteUI(getDesktopWorkbench(), JIPipeDesktopColorPaletteUI.NONE, JIPipeDesktopColorPalette.PASTEL);
        this.paletteUI.setUserColors(new JIPipeDesktopRegistryBackedColorPaletteUserColorStorage(paletteUI));
        this.paletteUI.getSelectedEventEmitter().subscribe(this);
        this.paletteUI.setSelectedColor(LAST_COLOR);

        // Initialize anchor
        anchorParameterEditorUI = JIPipe.getParameterTypes().createEditorInstanceWithEmbeddedValue(getDesktopWorkbench(), Anchor.class);
        anchorParameterEditorUI.getParameterAccess().getSource().getParameterChangedEventEmitter().subscribe(this);
        anchorParameterEditorUI.setParameter(LAST_ANCHOR, true);

        onPresetChanged();
    }

    private void renamePreset() {
        String newName = JOptionPane.showInputDialog(this, "Please enter the new name:", LAST_PRESET.getName());
        if(!StringUtils.isNullOrEmpty(newName)) {
            LAST_PRESET.setName(newName);
            onPresetChanged();
            savePresets();
        }
    }

    private void addPreset() {
        JIPipeNodeTemplate copy = new JIPipeNodeTemplate(LAST_PRESET);
        copy.setName("Untitled");
        TextBoxAnnotationGraphNode node = copy.getGraph().findFirstNodeOfType(TextBoxAnnotationGraphNode.class);
        if(node == null) {
            throw new RuntimeException("Cannot find node of type TextBoxAnnotationGraphNode");
        }
        if(JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), node, new MarkdownText(PRESET_DOCUMENTATION), "Add preset")) {
            PRESETS.add(copy);
            LAST_PRESET = copy;
            onPresetChanged();
            savePresets();
            renamePreset();
        }
    }

    private void deletePreset() {
        if(JOptionPane.showConfirmDialog(this,
                "Do you really want to delete the current preset?",
                "Delete preset",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            PRESETS.remove(LAST_PRESET);
            ensureNonEmptyPresets();
            onPresetChanged();
            savePresets();
        }
    }

    private void onPresetChanged() {
        pickPresetButton.setText(LAST_PRESET.getName());

        JIPipeNodeTemplate copy = new JIPipeNodeTemplate(LAST_PRESET);
        TextBoxAnnotationGraphNode node = copy.getGraph().findFirstNodeOfType(TextBoxAnnotationGraphNode.class);
        if(node != null) {
            getTool().setPreset(node);
        }
    }

    private void editPreset() {
        int index = PRESETS.indexOf(LAST_PRESET);
        if(index >= 0) {
            JIPipeNodeTemplate copy = new JIPipeNodeTemplate(LAST_PRESET);
            TextBoxAnnotationGraphNode node = copy.getGraph().findFirstNodeOfType(TextBoxAnnotationGraphNode.class);
            if(node == null) {
                throw new RuntimeException("Cannot find node of type TextBoxAnnotationGraphNode");
            }
            if(JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), node, new MarkdownText(PRESET_DOCUMENTATION), "Edit preset")) {
                PRESETS.set(index, copy);
                LAST_PRESET = copy;
                onPresetChanged();
                savePresets();
            }
        }
        else {
            throw new RuntimeException("Cannot find preset within list of available presets");
        }
    }

    private void pickPreset() {
        JIPipeNodeTemplatePickerDialog dialog = new JIPipeNodeTemplatePickerDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setAvailableItems(PRESETS);
        JIPipeNodeTemplate selected = dialog.showDialog();
        if(selected != null) {
            LAST_PRESET = selected;
            onPresetChanged();
        }
    }

    private void initializePreset() {
        if(PRESETS == null) {
            PRESETS =  JIPipe.getSettings().getListFromRegistry("ui-graph-editor",
                    Path.of("context-panel", "tools", "draw-text-box-annotation", "presets"),
                    JIPipeNodeTemplate.class,
                    true);
            PRESETS.removeIf(this::isInvalidPreset);
            ensureNonEmptyPresets();
        }
        if(LAST_PRESET == null) {
            LAST_PRESET = PRESETS.getFirst();
        }
    }

    private boolean isInvalidPreset(JIPipeNodeTemplate template) {
        JIPipeGraph graph = template.getGraph();
        if(graph == null ) {
            return true;
        }
        TextBoxAnnotationGraphNode node = graph.findFirstNodeOfType(TextBoxAnnotationGraphNode.class);
        return node == null;
    }

    private void savePresets() {
        JIPipe.getSettings().putIntoRegistry("ui-graph-editor",
                Path.of("context-panel", "tools", "draw-text-box-annotation", "presets"),
                PRESETS);
    }

    private static void ensureNonEmptyPresets() {
        if(PRESETS.isEmpty()) {
            JIPipeNodeTemplate template = new  JIPipeNodeTemplate();
            JIPipeGraph graph = new JIPipeGraph();
            graph.insertNode(JIPipe.createNode(TextBoxAnnotationGraphNode.class));
            template.setName("Default");
            template.setGraph(graph);
            PRESETS.add(template);
        }
    }

    @Override
    public final void initializeContent() {
        super.initializeContent();

        getFormPanel().addToForm(UIUtils.borderNSEWC(null, null, editPresetButton, null, pickPresetButton), new JLabel("Preset"));

        paletteUI.setBorder(UIUtils.createControlBorder());
        getFormPanel().addToForm(paletteUI, new JLabel("Color"));

        getFormPanel().addToForm(anchorParameterEditorUI, new JLabel("Text location"));

        getFormPanel().addWideToForm(UIUtils.createLeftAlignedButton("Close tool", JIPipe.RESOURCES.getIcon16("actions/message-close.png"), () -> {
            getGraphEditorUI().selectTool(null);
        }));
    }

    @Override
    public void onColorPaletteSelected(JIPipeDesktopColorPaletteUI.SelectedEvent event) {
        LAST_COLOR = event.getColor();
        getTool().setColor(event.getColor());
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        LAST_ANCHOR = anchorParameterEditorUI.getParameter();
        getTool().setAnchor(LAST_ANCHOR);
    }
}
