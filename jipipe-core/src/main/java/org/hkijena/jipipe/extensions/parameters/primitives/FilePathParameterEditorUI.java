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

package org.hkijena.jipipe.extensions.parameters.primitives;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;

import java.awt.*;
import java.nio.file.Path;

/**
 * Editor for a {@link Path} parameter
 */
public class FilePathParameterEditorUI extends JIPipeParameterEditorUI {

    private PathEditor pathEditor;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public FilePathParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
//        getWorkbenchUI().getProject().getEventBus().register(this);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        pathEditor.setPath(getParameterAccess().get(Path.class));
    }

    private void initialize() {
        setLayout(new BorderLayout());
        pathEditor = new PathEditor(PathEditor.IOMode.Open, PathEditor.PathMode.FilesOnly);
        FilePathParameterSettings settings = getParameterAccess().getAnnotationOfType(FilePathParameterSettings.class);
        if (settings != null) {
            pathEditor.setIoMode(settings.ioMode());
            pathEditor.setPathMode(settings.pathMode());
            pathEditor.setExtensionFilters(settings.extensions());
        }

        pathEditor.setPath(getParameterAccess().get(Path.class));
        add(pathEditor, BorderLayout.CENTER);
        pathEditor.addActionListener(e -> setParameter(pathEditor.getPath(), false));
    }

//    @Subscribe
//    public void onWorkDirectoryChanged(WorkDirectoryChangedEvent event) {
//        if (event.getWorkDirectory() != null)
//            fileSelection.getFileChooser().setCurrentDirectory(event.getWorkDirectory().toFile());
//    }
}
