package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.WorkDirectoryChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.components.FileSelection;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.scijava.Context;

import java.awt.*;

public class FilePathParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private FileSelection fileSelection;

    public FilePathParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        fileSelection.setPath(getParameterAccess().get());
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        fileSelection = new FileSelection(FileSelection.IOMode.Open, FileSelection.PathMode.FilesOnly);
        FilePathParameterSettings settings = getParameterAccess().getAnnotationOfType(FilePathParameterSettings.class);
        if (settings != null) {
            fileSelection.setIoMode(settings.ioMode());
            fileSelection.setPathMode(settings.pathMode());
        }

        fileSelection.setPath(getParameterAccess().get());
        add(fileSelection, BorderLayout.CENTER);
        fileSelection.addActionListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                if (!getParameterAccess().set(fileSelection.getPath())) {
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
