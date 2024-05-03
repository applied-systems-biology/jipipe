/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.library.filesystem;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPathEditorComponent;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.awt.*;
import java.nio.file.Path;

/**
 * Editor for a {@link Path} parameter
 */
public class FilePathDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private JIPipeDesktopPathEditorComponent pathEditor;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public FilePathDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
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
        pathEditor = new JIPipeDesktopPathEditorComponent(PathIOMode.Open, PathType.FilesOnly);
        PathParameterSettings settings = getParameterAccess().getAnnotationOfType(PathParameterSettings.class);
        if (settings != null) {
            pathEditor.setIoMode(settings.ioMode());
            pathEditor.setPathMode(settings.pathMode());
            pathEditor.setExtensionFilters(settings.extensions());
            if (JIPipe.isInstantiated()) {
                pathEditor.setDirectoryKey(settings.key());
            }
        }

        pathEditor.setPath(getParameterAccess().get(Path.class));
        add(pathEditor, BorderLayout.CENTER);
        pathEditor.addActionListener(e -> setParameter(pathEditor.getPath(), false));
    }

//    @Override
//    public void onWorkDirectoryChanged(WorkDirectoryChangedEvent event) {
//        if (event.getWorkDirectory() != null)
//            fileSelection.getFileChooser().setCurrentDirectory(event.getWorkDirectory().toFile());
//    }
}
