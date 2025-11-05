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
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * Provides an input file
 */
@SetJIPipeDocumentation(name = "File", description = "Converts the path parameter into file data.")
@AddJIPipeOutputSlot(value = FileData.class, name = "Filename", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class FileDataSource extends AbstractPathDataSource {

    private Path fileName;

    public FileDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public FileDataSource(FileDataSource other) {
        super(other);
        this.fileName = other.fileName;
    }

    /**
     * @return The file name
     */
    @JIPipeParameter("file-name")
    @SetJIPipeDocumentation(name = "File name")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.FilesOnly)
    public Path getFileName() {
        return fileName;
    }

    /**
     * Sets the file name
     *
     * @param fileName The file name
     */
    @JIPipeParameter("file-name")
    public void setFileName(Path fileName) {
        this.fileName = PathUtils.normalize(fileName);
        updateOutputSlotIfEnabled();
    }

    @Override
    protected List<Path> getPaths_() {
        if (fileName != null) {
            return List.of(fileName);
        } else {
            return List.of();
        }
    }

    @Override
    protected void setPaths_(List<Path> paths) {
        if (paths.isEmpty()) {
            setFileName(null);
        } else {
            setFileName(paths.getFirst());
        }
    }
}
