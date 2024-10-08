package org.hkijena.jipipe.plugins.filesystem.desktop;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFancyReadOnlyTextField;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathDataViewer extends JIPipeDesktopDataViewer {

    private final JIPipeDesktopFancyReadOnlyTextField pathTextField = new JIPipeDesktopFancyReadOnlyTextField(LOADING_PLACEHOLDER_TEXT, true);
    private final JIPipeDesktopFancyReadOnlyTextField pathParentTextField = new JIPipeDesktopFancyReadOnlyTextField(LOADING_PLACEHOLDER_TEXT, true);
    private final JLabel pathTypeLabel = new JLabel();
    private String currentPath;

    public PathDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        initialize();
    }

    private void initialize() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        formPanel.addToForm(pathTextField, new JLabel("Path"));
        formPanel.addToForm(pathParentTextField, new JLabel("Parent path"));
        formPanel.addToForm(pathTypeLabel, new JLabel("Type"));
        add(formPanel, BorderLayout.CENTER);
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task generalTask = ribbon.getOrCreateTask("General");
        JIPipeDesktopRibbon.Band pathBand = generalTask.getOrCreateBand("Path");
        pathBand.add(new JIPipeDesktopLargeButtonRibbonAction("Open path", "Opens the path", UIUtils.getIcon32FromResources("actions/document-open.png"), this::openPath));
        pathBand.add(new JIPipeDesktopLargeButtonRibbonAction("Open parent", "Opens the path's parent directory", UIUtils.getIcon32FromResources("actions/go-parent-folder.png"), this::openParentPath));
    }

    private void openParentPath() {
        try {
            Desktop.getDesktop().open(new File(currentPath).getParentFile());
        } catch (IOException e) {
            UIUtils.openErrorDialog(getDesktopWorkbench(), this, e);
        }
    }

    private void openPath() {
        try {
            Desktop.getDesktop().open(new File(currentPath));
        } catch (IOException e) {
            UIUtils.openErrorDialog(getDesktopWorkbench(), this, e);
        }
    }

    @Override
    public void postOnDataChanged() {
        awaitToSwing(getDataBrowser().getData(PathData.class), data -> {
            currentPath = data.getPath();
            pathTextField.setText(currentPath);
            try {
                Path path = Paths.get(currentPath);
                pathParentTextField.setText(path.getParent().toString());
                if(Files.isRegularFile(path)) {
                    pathTypeLabel.setText("File");
                    pathTypeLabel.setIcon(UIUtils.getIconFromResources("actions/file.png"));
                }
                else if(Files.isDirectory(path)) {
                    pathTypeLabel.setText("Folder");
                    pathTypeLabel.setIcon(UIUtils.getIconFromResources("actions/folder.png"));
                }
                else {
                    pathTypeLabel.setText("Unknown");
                    pathTypeLabel.setIcon(UIUtils.getIconFromResources("actions/circle-question.png"));
                }
            }
            catch (Exception e) {
                pathParentTextField.setText(ERROR_PLACEHOLDER_TEXT);
                pathTypeLabel.setText(ERROR_PLACEHOLDER_TEXT);
            }
        });
    }
}
