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

package org.hkijena.jipipe.plugins.filesystem.datasources;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDummyWorkbench;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SetJIPipeDocumentation(name = "Select path (interactive)", description = "Asks for a path (file/folder) interactively when the node is run.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = PathData.class, slotName = "Path", create = true)
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

    @SetJIPipeDocumentation(name = "IO type", description = "Determines if the path is opened or saved.")
    @JIPipeParameter("path-io-mode")
    public PathIOMode getPathIOMode() {
        return pathIOMode;
    }

    @JIPipeParameter("path-io-mode")
    public void setPathIOMode(PathIOMode pathIOMode) {
        this.pathIOMode = pathIOMode;
    }

    @SetJIPipeDocumentation(name = "Path type", description = "The type of the path")
    @JIPipeParameter("path-type")
    public PathType getPathType() {
        return pathType;
    }

    @JIPipeParameter("path-type")
    public void setPathType(PathType pathType) {
        this.pathType = pathType;
    }

    @SetJIPipeDocumentation(name = "Select multiple paths", description = "If enabled, the user can select multiple paths")
    @JIPipeParameter("multiple")
    public boolean isMultiple() {
        return multiple;
    }

    @JIPipeParameter("multiple")
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        AtomicBoolean windowOpened = new AtomicBoolean(true);
        List<Path> pathList = new ArrayList<>();
        Object lock = new Object();

        synchronized (lock) {
            SwingUtilities.invokeLater(() -> {
                try {
                    JIPipeWorkbench workbench = JIPipeDesktopProjectWorkbench.tryFindProjectWorkbench(getParentGraph(), new JIPipeDummyWorkbench());
                    if (multiple) {
                        pathList.addAll(FileChooserSettings.selectMulti(((JIPipeDesktopWorkbench) workbench).getWindow(),
                                FileChooserSettings.LastDirectoryKey.Data,
                                getDisplayName(),
                                pathIOMode,
                                pathType));
                    } else {
                        Path path = FileChooserSettings.selectSingle(((JIPipeDesktopWorkbench) workbench).getWindow(),
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
            throw new JIPipeValidationRuntimeException(new InterruptedException(),
                    "User input was cancelled!",
                    "You had to provide input to allow the pipeline to continue. Instead, you cancelled the input.",
                    "");
        }

        for (Path path : pathList) {
            iterationStep.addOutputData(getFirstOutputSlot(), new PathData(path), progressInfo);
        }
    }
}
