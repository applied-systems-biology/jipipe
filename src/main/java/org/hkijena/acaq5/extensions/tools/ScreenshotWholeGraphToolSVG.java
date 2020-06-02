package org.hkijena.acaq5.extensions.tools;

import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.extension.MenuExtension;
import org.hkijena.acaq5.ui.extension.MenuTarget;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import javax.swing.*;
import java.io.IOException;

@ACAQOrganization(menuExtensionTarget = MenuTarget.ProjectToolsMenu, menuPath = "Export full graph")
public class ScreenshotWholeGraphToolSVG extends MenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ScreenshotWholeGraphToolSVG(ACAQWorkbench workbench) {
        super(workbench);
        setText("As *.svg");
        setToolTipText("Creates a screenshot of the whole pipeline graph. Nodes are automatically aligned.");
        setIcon(UIUtils.getIconFromResources("filetype-image.png"));
        addActionListener(e -> createScreenshot());
    }

    private void createScreenshot() {
        ACAQProjectWorkbench workbench = (ACAQProjectWorkbench) getWorkbench();
        ACAQAlgorithmGraphCanvasUI canvasUI = new ACAQAlgorithmGraphCanvasUI(workbench.getProject().getGraph(), null);
        canvasUI.autoLayoutAll();
        SVGGraphics2D screenshot = canvasUI.createScreenshotSVG();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export full graph as *.svg");
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                SVGUtils.writeToSVG(fileChooser.getSelectedFile(), screenshot.getSVGElement());
                getWorkbench().sendStatusBarText("Exported full graph as " + fileChooser.getSelectedFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
