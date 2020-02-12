package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.FileSelection;
import org.hkijena.acaq5.ui.grapheditor.ACAQParameterEditorUI;

import java.awt.*;
import java.nio.file.Path;

public class FilePathParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQParameterAccess.Instance<Path> filePathAccess;

    public FilePathParameterEditorUI(ACAQAlgorithm algorithm, ACAQParameterAccess parameterAccess) {
        super(algorithm, parameterAccess);
        filePathAccess = parameterAccess.instantiate(algorithm);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        FileSelection fileSelection = new FileSelection(FileSelection.Mode.OPEN);
        fileSelection.setPath(filePathAccess.get());
        add(fileSelection, BorderLayout.CENTER);
        fileSelection.addActionListener(e -> filePathAccess.set(fileSelection.getPath()));
    }
}
