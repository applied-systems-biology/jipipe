package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Select path (interactive)", description = "Asks for a path (file/folder) interactively when the node is run.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = PathData.class, slotName = "Path", autoCreate = true)
public class PathFromUserDataSource extends JIPipeSimpleIteratingAlgorithm {

    private PathIOMode pathIOMode = PathIOMode.Open;
    private PathType pathType = PathType.FilesAndDirectories;
    private boolean multiple = true;

    public PathFromUserDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public PathFromUserDataSource(PathFromUserDataSource other) {
        super(other);
        this.pathIOMode = other.pathIOMode;
        this.pathType = other.pathType;
        this.multiple = other.multiple;
    }

    @JIPipeDocumentation(name = "IO type", description = "Determines if the path is opened or saved.")
    @JIPipeParameter("path-io-mode")
    public PathIOMode getPathIOMode() {
        return pathIOMode;
    }

    @JIPipeParameter("path-io-mode")
    public void setPathIOMode(PathIOMode pathIOMode) {
        this.pathIOMode = pathIOMode;
    }

    @JIPipeDocumentation(name = "Path type", description = "The type of the path")
    @JIPipeParameter("path-type")
    public PathType getPathType() {
        return pathType;
    }

    @JIPipeParameter("path-type")
    public void setPathType(PathType pathType) {
        this.pathType = pathType;
    }

    @JIPipeDocumentation(name = "Select multiple paths", description = "If enabled, the user can select multiple paths")
    @JIPipeParameter("multiple")
    public boolean isMultiple() {
        return multiple;
    }

    @JIPipeParameter("multiple")
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        AtomicBoolean windowOpened = new AtomicBoolean(true);
        List<Path> pathList = new ArrayList<>();
        Object lock = new Object();

        synchronized (lock) {
            SwingUtilities.invokeLater(() -> {
                try {
                    JIPipeWorkbench workbench = JIPipeWorkbench.tryFindWorkbench(getGraph(), new JIPipeDummyWorkbench());
                    if (multiple) {
                        pathList.addAll(FileChooserSettings.selectMulti(workbench.getWindow(),
                                FileChooserSettings.LastDirectoryKey.Data,
                                getDisplayName(),
                                pathIOMode,
                                pathType));
                    } else {
                        Path path = FileChooserSettings.selectSingle(workbench.getWindow(),
                                FileChooserSettings.LastDirectoryKey.Data,
                                getDisplayName(),
                                pathIOMode,
                                pathType);
                        if (path != null)
                            pathList.add(path);
                    }
                    windowOpened.set(false);
                    synchronized (lock) {
                        lock.notify();
                    }
                } catch (Throwable e) {
                    windowOpened.set(false);
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            });

            try {
                while (windowOpened.get()) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (pathList.isEmpty()) {
            progressInfo.log("User input was cancelled!");
            throw new UserFriendlyRuntimeException("User input was cancelled!",
                    "User input was cancelled!",
                    "Node '" + getName() + "'",
                    "You had to provide input to allow the pipeline to continue. Instead, you cancelled the input.",
                    "");
        }

        for (Path path : pathList) {
            dataBatch.addOutputData(getFirstOutputSlot(), new PathData(path), progressInfo);
        }
    }
}
