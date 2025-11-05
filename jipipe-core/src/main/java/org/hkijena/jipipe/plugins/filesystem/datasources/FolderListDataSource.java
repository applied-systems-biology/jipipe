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
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathList;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * Provides an input folder
 */
@SetJIPipeDocumentation(name = "Folder list", description = "Converts each provided path into folder data.")
@AddJIPipeOutputSlot(value = FolderData.class, name = "Folder paths", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class FolderListDataSource extends AbstractPathDataSource {

    private PathList folderPaths = new PathList();

    public FolderListDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public FolderListDataSource(FolderListDataSource other) {
        super(other);
        this.folderPaths.addAll(other.folderPaths);
    }

    /**
     * @return Gets the folder paths
     */
    @JIPipeParameter("folder-paths")
    @SetJIPipeDocumentation(name = "Folder paths")
    @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.DirectoriesOnly)
    @ListParameterSettings(withScrollBar = true)
    public PathList getFolderPaths() {
        return folderPaths;
    }

    /**
     * Sets the folder path
     *
     * @param folderPaths Folder paths
     */
    @JIPipeParameter("folder-paths")
    public void setFolderPaths(PathList folderPaths) {
        this.folderPaths = folderPaths;
        PathUtils.normalizeList(folderPaths);
        updateOutputSlotIfEnabled();
    }

    @Override
    protected List<Path> getPaths_() {
        return folderPaths;
    }

    @Override
    protected void setPaths_(List<Path> paths) {
        setFolderPaths(new PathList(paths));
    }

}
