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

import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.plugins.filesystem.JIPipeFilesystemPluginApplicationSettings;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Provides an input file
 */
@SetJIPipeDocumentation(name = "Path", description = "Converts the path parameter into path data.")
@AddJIPipeOutputSlot(value = PathData.class, name = "Path", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class PathDataSource extends AbstractPathDataSource {

    private Path path;

    /**
     * Initializes the algorithm
     *
     * @param info The algorithm info
     */
    public PathDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public PathDataSource(PathDataSource other) {
        super(other);
        this.path = other.path;
    }

    /**
     * @return The file name
     */
    @JIPipeParameter("path")
    @SetJIPipeDocumentation(name = "Path")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.FilesAndDirectories)
    public Path getPath() {
        return path;
    }

    /**
     * Sets the file name
     *
     * @param path The file name
     */
    @JIPipeParameter("path")
    public void setPath(Path path) {
        this.path = PathUtils.normalize(path);
        updateOutputSlotIfEnabled();
    }

    @Override
    protected List<Path> getPaths_() {
        if(path != null) {
            return List.of(path);
        }
        else {
            return List.of();
        }
    }

    @Override
    protected void setPaths_(List<Path> paths) {
        if(paths.isEmpty()) {
            setPath(null);
        }
        else {
            setPath(paths.getFirst());
        }
    }


}
