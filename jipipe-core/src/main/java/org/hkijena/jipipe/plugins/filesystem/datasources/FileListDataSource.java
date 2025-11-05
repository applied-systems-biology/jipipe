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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathList;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * Provides an input file
 */
@SetJIPipeDocumentation(name = "File list", description = "Converts each provided path into file data.")
@AddJIPipeOutputSlot(value = FileData.class, name = "Filenames", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class FileListDataSource extends AbstractPathDataSource {

    private PathList files = new PathList();

    /**
     * Initializes the algorithm
     *
     * @param info The algorithm info
     */
    public FileListDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public FileListDataSource(FileListDataSource other) {
        super(other);
        this.files.addAll(other.files);
    }

    /**
     * @return The file names
     */
    @JIPipeParameter("file-names")
    @SetJIPipeDocumentation(name = "Files")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.FilesOnly)
    @ListParameterSettings(withScrollBar = true)
    public PathList getFiles() {
        return files;
    }

    /**
     * Sets the file names
     *
     * @param files The file names
     */
    @JIPipeParameter("file-names")
    public void setFiles(PathList files) {
        this.files = files;
        PathUtils.normalizeList(files);
        updateOutputSlotIfEnabled();
    }

    @Override
    protected List<Path> getPaths_() {
        return files;
    }

    @Override
    protected void setPaths_(List<Path> paths) {
        setFiles(new PathList(paths));
    }
}
