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

import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPathEditorComponent;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.awt.*;
import java.io.File;

/**
 * Editor for a {@link File} parameter
 */
public class FileDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private JIPipeDesktopPathEditorComponent pathEditor;

    public FileDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
//        getWorkbenchUI().getProject().getEventBus().register(this);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        pathEditor.setPath(getParameter(File.class).toPath());
    }

    private void initialize() {
        setLayout(new BorderLayout());
        pathEditor = new JIPipeDesktopPathEditorComponent(getDesktopWorkbench(), PathIOMode.Open, PathType.FilesOnly);
        PathParameterSettings settings = getParameterAccess().getAnnotationOfType(PathParameterSettings.class);
        if (settings != null) {
            pathEditor.setIoMode(settings.ioMode());
            pathEditor.setPathMode(settings.pathMode());
        }

        pathEditor.setPath(getParameter(File.class).toPath());
        add(pathEditor, BorderLayout.CENTER);
        pathEditor.addActionListener(e -> {
            setParameter(pathEditor.getPath().toFile(), false);
        });
    }

//    @Override
//    public void onWorkDirectoryChanged(WorkDirectoryChangedEvent event) {
//        if (event.getWorkDirectory() != null)
//            fileSelection.getFileChooser().setCurrentDirectory(event.getWorkDirectory().toFile());
//    }
}
