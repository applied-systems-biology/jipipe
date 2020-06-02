package org.hkijena.acaq5.extensions.tools;

import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.extension.MenuExtension;
import org.hkijena.acaq5.ui.extension.MenuTarget;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

@ACAQOrganization(menuExtensionTarget = MenuTarget.ProjectToolsMenu, menuPath = "Export full graph")
public class ScreenshotWholeGraphToolPNG extends MenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ScreenshotWholeGraphToolPNG(ACAQWorkbench workbench) {
        super(workbench);
        setText("As *.png");
        setToolTipText("Creates a screenshot of the whole pipeline graph. Nodes are automatically aligned.");
        setIcon(UIUtils.getIconFromResources("filetype-image.png"));
        addActionListener(e -> createScreenshot());
    }

    private void createScreenshot() {
        ACAQProjectWorkbench workbench = (ACAQProjectWorkbench) getWorkbench();
        ACAQAlgorithmGraphCanvasUI canvasUI = new ACAQAlgorithmGraphCanvasUI(workbench.getProject().getGraph(), null);
        canvasUI.autoLayoutAll();
        BufferedImage screenshot = canvasUI.createScreenshotPNG();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export full graph as *.png");
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(screenshot, "PNG", fileChooser.getSelectedFile());
                getWorkbench().sendStatusBarText("Exported full graph as " + fileChooser.getSelectedFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
