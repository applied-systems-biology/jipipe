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
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopGraphEditorToolPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.colorpalette.JIPipeDesktopColorPalette;
import org.hkijena.jipipe.desktop.commons.components.colorpalette.JIPipeDesktopColorPaletteColor;
import org.hkijena.jipipe.desktop.commons.components.colorpalette.JIPipeDesktopColorPaletteUI;
import org.hkijena.jipipe.desktop.commons.components.colorpalette.JIPipeDesktopSettingsBackedColorPaletteUserColorStorage;
import org.hkijena.jipipe.utils.UIUtils;

public class ImageBoxAnnotationGraphNodeToolProperties extends JIPipeDesktopFormGraphEditorToolPanel<ImageBoxAnnotationGraphNodeTool> implements JIPipeDesktopColorPaletteUI.SelectedEventListener {

    private final JIPipeDesktopColorPaletteUI paletteUI;
    private static JIPipeDesktopColorPaletteColor LAST_COLOR = JIPipeDesktopColorPalette.PASTEL[0];

    public ImageBoxAnnotationGraphNodeToolProperties(JIPipeDesktopGraphEditorUI graphEditorUI, ImageBoxAnnotationGraphNodeTool tool) {
        super(graphEditorUI, tool);
        this.paletteUI = new JIPipeDesktopColorPaletteUI(getDesktopWorkbench(), JIPipeDesktopColorPaletteUI.NONE, JIPipeDesktopColorPalette.PASTEL);
        this.paletteUI.setUserColors(new JIPipeDesktopSettingsBackedColorPaletteUserColorStorage(paletteUI));
        this.paletteUI.getSelectedEventEmitter().subscribe(this);
        this.paletteUI.setSelectedColor(LAST_COLOR);
    }

    @Override
    public final void initializeContent() {
        super.initializeContent();

        paletteUI.setBorder(UIUtils.createControlBorder());
        getFormPanel().addWideToForm(paletteUI);

        getFormPanel().addWideToForm(UIUtils.createLeftAlignedButton("Close tool", JIPipe.RESOURCES.getIcon16("actions/message-close.png"), () -> {
            getGraphEditorUI().selectTool(null);
        }));
    }

    @Override
    public void onColorPaletteSelected(JIPipeDesktopColorPaletteUI.SelectedEvent event) {
        LAST_COLOR = event.getColor();
        getTool().setColor(event.getColor());
    }
}
