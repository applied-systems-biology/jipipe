package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.plugins.expressions.ui.JIPipeExpressionDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;

public class DataExportExpressionParameterEditorUI extends JIPipeExpressionDesktopParameterEditorUI {
    public DataExportExpressionParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
    }

    private void initialize() {
        JButton setPathButton = new JButton("Select", UIUtils.getIconFromResources("actions/fileopen.png"));
        setPathButton.addActionListener(e -> { openPath(); });
        getEditPanel().add(setPathButton);
    }

    private void openPath() {
        PathIOMode ioMode = PathIOMode.Save;
        PathType pathType = PathType.FilesAndDirectories;
        String[] extensions = new String[] {};
        PathParameterSettings settings = getParameterAccess().getAnnotationOfType(PathParameterSettings.class);
        if(settings != null) {
            ioMode = settings.ioMode();
            pathType = settings.pathMode();
            extensions = settings.extensions();
        }

        DataExportExpressionParameter selectedPath;
        if(extensions == null || extensions.length == 0) {
            selectedPath = DataExportExpressionParameter.showPathChooser(getDesktopWorkbench().getWindow(), getWorkbench(), "Select path", pathType);
        }
        else {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Supported files", extensions);
            selectedPath = DataExportExpressionParameter.showPathChooser(getDesktopWorkbench().getWindow(), getWorkbench(), "Select path", pathType, filter);
        }

        if (selectedPath != null) {
            setParameter(selectedPath, true);
        }
    }
}
