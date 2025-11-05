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
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopFormGraphEditorToolPanel;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPalette;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPaletteColor;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopColorPaletteUI;
import org.hkijena.jipipe.desktop.commons.components.color.palette.JIPipeDesktopRegistryBackedColorPaletteUserColorStorage;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class TextBoxAnnotationGraphNodeToolProperties extends JIPipeDesktopFormGraphEditorToolPanel<TextBoxAnnotationGraphNodeTool> implements JIPipeDesktopColorPaletteUI.SelectedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {

    private static JIPipeDesktopColorPaletteColor LAST_COLOR = JIPipeDesktopColorPalette.PASTEL[0];
    private static Anchor LAST_ANCHOR = Anchor.CenterCenter;
    private final JIPipeDesktopColorPaletteUI paletteUI;
    private final JIPipeDesktopParameterEditorUI<Anchor> anchorParameterEditorUI;

    public TextBoxAnnotationGraphNodeToolProperties(JIPipeDesktopGraphEditorUI graphEditorUI, TextBoxAnnotationGraphNodeTool tool) {
        super(graphEditorUI, tool);

        // Initialize palette
        this.paletteUI = new JIPipeDesktopColorPaletteUI(getDesktopWorkbench(), JIPipeDesktopColorPaletteUI.NONE, JIPipeDesktopColorPalette.PASTEL);
        this.paletteUI.setUserColors(new JIPipeDesktopRegistryBackedColorPaletteUserColorStorage(paletteUI));
        this.paletteUI.getSelectedEventEmitter().subscribe(this);
        this.paletteUI.setSelectedColor(LAST_COLOR);

        // Initialize anchor
        anchorParameterEditorUI = JIPipe.getParameterTypes().createEditorInstanceWithEmbeddedValue(getDesktopWorkbench(), Anchor.class);
        anchorParameterEditorUI.getParameterAccess().getSource().getParameterChangedEventEmitter().subscribe(this);
        anchorParameterEditorUI.setParameter(LAST_ANCHOR, true);
    }

    @Override
    public final void initializeContent() {
        super.initializeContent();

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
