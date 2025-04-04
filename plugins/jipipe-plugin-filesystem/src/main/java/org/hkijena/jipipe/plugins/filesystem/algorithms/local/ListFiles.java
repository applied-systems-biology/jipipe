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

package org.hkijena.jipipe.plugins.filesystem.algorithms.local;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.PathQueryExpression;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Algorithm that lists files in each folder
 */
@SetJIPipeDocumentation(name = "List files", description = "Lists all files in the input folder")
@ConfigureJIPipeNode(menuPath = "List", nodeTypeCategory = FileSystemNodeTypeCategory.class)


@AddJIPipeInputSlot(value = FolderData.class, name = "Folders", create = true)
@AddJIPipeOutputSlot(value = FileData.class, name = "Files", create = true)


public class ListFiles extends JIPipeSimpleIteratingAlgorithm {

    private PathQueryExpression filters = new PathQueryExpression();
    private String subFolder;
    private boolean recursive = false;
    private boolean recursiveFollowsLinks = true;

    public ListFiles(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ListFiles(ListFiles other) {
        super(other);
        this.filters = new PathQueryExpression(other.filters);
        this.subFolder = other.subFolder;
        this.recursive = other.recursive;
        this.recursiveFollowsLinks = other.recursiveFollowsLinks;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Expression parameters from annotations
        JIPipeExpressionVariablesMap expressionVariables = new JIPipeExpressionVariablesMap(iterationStep);

        FolderData inputFolder = iterationStep.getInputData(getFirstInputSlot(), FolderData.class, progressInfo);
        Path inputPath = inputFolder.toPath();
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
                stream = Files.walk(inputPath, options).filter(Files::isRegularFile);
            } else
                stream = Files.list(inputPath).filter(Files::isRegularFile);
            for (Path file : stream.collect(Collectors.toList())) {
                if (filters.test(file, expressionVariables)) {
                    iterationStep.addOutputData(getFirstOutputSlot(), new FileData(file), progressInfo);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SetJIPipeDocumentation(name = "Keep file if ...", description = "Filter expression that allows to filter the files. " +
            "Click the [X] button to see all available variables. " +
            "An example for an expression would be '\".tif\" IN name', which would test if there is '.tif' inside the file name. " +
            "Annotations are available as variables.")
    @JIPipeParameter("filters")
    public PathQueryExpression getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(PathQueryExpression filters) {
        this.filters = filters;
    }

    @SetJIPipeDocumentation(name = "Subfolder", description = "Optional. If non-empty, all files are extracted from the provided sub-folder. " +
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

    @SetJIPipeDocumentation(name = "Recursive", description = "If enabled, the search is recursive.")
    @JIPipeParameter(value = "recursive", important = true)
    public boolean isRecursive() {
        return recursive;
    }

    @JIPipeParameter("recursive")
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    @SetJIPipeDocumentation(name = "Recursive search follows links", description = "If enabled, a recursive search follows symbolic links. " +
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
