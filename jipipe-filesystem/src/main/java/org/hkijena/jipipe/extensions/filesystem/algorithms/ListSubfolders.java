/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.parameters.expressions.variables.PathFilterExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Algorithms that lists the sub folders for each input folder
 */
@JIPipeDocumentation(name = "List subfolders", description = "Lists all subfolders")
@JIPipeOrganization(menuPath = "List", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@JIPipeInputSlot(value = FolderData.class, slotName = "Folders", autoCreate = true)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Subfolders", autoCreate = true)

// Traits
public class ListSubfolders extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter filters = new DefaultExpressionParameter("");
    private String subFolder;
    private boolean recursive = false;
    private boolean recursiveFollowsLinks = true;

    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public ListSubfolders(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ListSubfolders(ListSubfolders other) {
        super(other);
        this.subFolder = other.subFolder;
        this.recursive = other.recursive;
        this.recursiveFollowsLinks = other.recursiveFollowsLinks;
        this.filters = new DefaultExpressionParameter(other.filters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        FolderData inputFolder = dataBatch.getInputData(getFirstInputSlot(), FolderData.class);
        Path inputPath = inputFolder.getPath();
        if (!StringUtils.isNullOrEmpty(subFolder)) {
            inputPath = inputPath.resolve(subFolder);
        }
        try {
            Stream<Path> stream;
            if (recursive) {
                FileVisitOption[] options;
                if (recursiveFollowsLinks)
                    options = new FileVisitOption[]{FileVisitOption.FOLLOW_LINKS};
                else
                    options = new FileVisitOption[0];
                stream = Files.walk(inputPath, options).filter(Files::isDirectory);
            } else
                stream = Files.list(inputPath).filter(Files::isDirectory);
            for (Path file : stream.collect(Collectors.toList())) {
                if (filters.test(PathFilterExpressionParameterVariableSource.buildFor(file))) {
                    dataBatch.addOutputData(getFirstOutputSlot(), new FileData(file));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JIPipeDocumentation(name = "Filters", description = "You can optionally filter the result folders. " +
            "The filters are connected via a logical OR operation. An empty list disables filtering")
    @JIPipeParameter("filters")
    @ExpressionParameterSettings(variableSource = PathFilterExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(DefaultExpressionParameter filters) {
        this.filters = filters;
    }

    @JIPipeDocumentation(name = "Subfolder", description = "Optional. If non-empty, all files are extracted from the provided sub-folder. " +
            "The sub-folder navigation is applied before recursive search (if 'Recursive' is enabled).")
    @JIPipeParameter("subfolder")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/folder-open.png")
    public String getSubFolder() {
        return subFolder;
    }

    @JIPipeParameter("subfolder")
    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }

    @JIPipeDocumentation(name = "Recursive", description = "If enabled, the search is recursive.")
    @JIPipeParameter("recursive")
    public boolean isRecursive() {
        return recursive;
    }

    @JIPipeParameter("recursive")
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    @JIPipeDocumentation(name = "Recursive search follows links", description = "If enabled, a recursive search follows symbolic links. " +
            "(Only Windows) Please note that Windows does not create symbolic links by default.")
    @JIPipeParameter("recursive-follows-links")
    public boolean isRecursiveFollowsLinks() {
        return recursiveFollowsLinks;
    }

    @JIPipeParameter("recursive-follows-links")
    public void setRecursiveFollowsLinks(boolean recursiveFollowsLinks) {
        this.recursiveFollowsLinks = recursiveFollowsLinks;
    }
}
