package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.FileSelection;
import org.hkijena.acaq5.ui.grapheditor.ACAQParameterEditorUI;

import java.awt.*;
import java.nio.file.Path;

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
        FileSelection fileSelection = new FileSelection(FileSelection.Mode.OPEN);
        fileSelection.setPath(getParameterAccess().get());
        add(fileSelection, BorderLayout.CENTER);
        fileSelection.addActionListener(e -> getParameterAccess().set(fileSelection.getPath()));
    }
}
