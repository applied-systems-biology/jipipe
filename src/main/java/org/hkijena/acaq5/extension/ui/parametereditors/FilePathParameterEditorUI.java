package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.FileSelection;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import java.awt.BorderLayout;

public class FilePathParameterEditorUI extends ACAQParameterEditorUI {

    public FilePathParameterEditorUI(ACAQParameterAccess parameterAccess) {
        super(parameterAccess);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        FileSelection fileSelection = new FileSelection(FileSelection.IOMode.Open, FileSelection.PathMode.FilesOnly);
        FilePathParameterSettings settings = getParameterAccess().getAnnotationOfType(FilePathParameterSettings.class);
        if (settings != null) {
            fileSelection.setIoMode(settings.ioMode());
            fileSelection.setPathMode(settings.pathMode());
        }

        fileSelection.setPath(getParameterAccess().get());
        add(fileSelection, BorderLayout.CENTER);
        fileSelection.addActionListener(e -> {
            if (!getParameterAccess().set(fileSelection.getPath())) {
                fileSelection.setPath(fileSelection.getPath());
            }
        });
    }
}
