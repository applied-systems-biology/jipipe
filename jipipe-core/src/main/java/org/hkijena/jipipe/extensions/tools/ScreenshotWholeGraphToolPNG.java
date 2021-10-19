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

import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ScreenshotWholeGraphToolPNG extends JIPipeMenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ScreenshotWholeGraphToolPNG(JIPipeWorkbench workbench) {
        super(workbench);
        setText("As *.png");
        setToolTipText("Creates a screenshot of the whole pipeline graph. Nodes are automatically aligned.");
        setIcon(UIUtils.getIconFromResources("actions/viewimage.png"));
        addActionListener(e -> createScreenshot());
    }

    private void createScreenshot() {
        JIPipeProjectWorkbench workbench = (JIPipeProjectWorkbench) getWorkbench();
        JIPipeGraphCanvasUI canvasUI = new JIPipeGraphCanvasUI(workbench, workbench.getProject().getGraph(), null);
        canvasUI.autoLayoutAll();
        BufferedImage screenshot = canvasUI.createScreenshotPNG();
        Path file = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Parameters, "Export full graph as *.png", UIUtils.EXTENSION_FILTER_PNG);
        if (file != null) {
            try {
                ImageIO.write(screenshot, "PNG", file.toFile());
                getWorkbench().sendStatusBarText("Exported full graph as " + file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Export full graph";
    }
}
