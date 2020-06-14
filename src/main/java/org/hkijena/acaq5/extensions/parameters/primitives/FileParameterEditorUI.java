package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.PathEditor;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.scijava.Context;

import java.awt.*;
import java.io.File;

/**
 * Editor for a {@link java.io.File} parameter
 */
public class FileParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private PathEditor pathEditor;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public FileParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
//        getWorkbenchUI().getProject().getEventBus().register(this);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        pathEditor.setPath(getParameter(File.class).toPath());
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        pathEditor = new PathEditor(PathEditor.IOMode.Open, PathEditor.PathMode.FilesOnly);
        FilePathParameterSettings settings = getParameterAccess().getAnnotationOfType(FilePathParameterSettings.class);
        if (settings != null) {
            pathEditor.setIoMode(settings.ioMode());
            pathEditor.setPathMode(settings.pathMode());
        }

        pathEditor.setPath(getParameter(File.class).toPath());
        add(pathEditor, BorderLayout.CENTER);
        pathEditor.addActionListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                if (!getParameterAccess().set(pathEditor.getPath().toFile())) {
                    skipNextReload = false;
                    reload();
                }
            }
        });
    }

//    @Subscribe
//    public void onWorkDirectoryChanged(WorkDirectoryChangedEvent event) {
//        if (event.getWorkDirectory() != null)
//            fileSelection.getFileChooser().setCurrentDirectory(event.getWorkDirectory().toFile());
//    }
}
