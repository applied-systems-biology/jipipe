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

package org.hkijena.jipipe.extensions.tools;

import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import java.io.IOException;
import java.nio.file.Path;

@JIPipeOrganization(menuExtensionTarget = MenuTarget.ProjectToolsMenu, menuPath = "Export full graph")
public class ScreenshotWholeGraphToolSVG extends MenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ScreenshotWholeGraphToolSVG(JIPipeWorkbench workbench) {
        super(workbench);
        setText("As *.svg");
        setToolTipText("Creates a screenshot of the whole pipeline graph. Nodes are automatically aligned.");
        setIcon(UIUtils.getIconFromResources("actions/viewimage.png"));
        addActionListener(e -> createScreenshot());
    }

    private void createScreenshot() {
        JIPipeProjectWorkbench workbench = (JIPipeProjectWorkbench) getWorkbench();
        JIPipeGraphCanvasUI canvasUI = new JIPipeGraphCanvasUI(workbench, workbench.getProject().getGraph(), null);
        canvasUI.autoLayoutAll();
        SVGGraphics2D screenshot = canvasUI.createScreenshotSVG();
        Path file = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PARAMETER, "Export full graph as *.svg", UIUtils.EXTENSION_FILTER_SVG);
        if (file != null) {
            try {
                SVGUtils.writeToSVG(file.toFile(), screenshot.getSVGElement());
                getWorkbench().sendStatusBarText("Exported full graph as " + file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
