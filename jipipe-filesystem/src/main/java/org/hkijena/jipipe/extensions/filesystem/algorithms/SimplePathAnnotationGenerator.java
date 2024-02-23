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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;

/**
 * Algorithm that generates annotations from folder names
 */
@SetJIPipeDocumentation(name = "Add path to annotations", description = "Creates an annotation for each path based on its name or its full path.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For paths")
@AddJIPipeInputSlot(value = PathData.class, slotName = "Paths", create = true)
@AddJIPipeOutputSlot(value = PathData.class, slotName = "Annotated paths", create = true)
public class SimplePathAnnotationGenerator extends JIPipeSimpleIteratingAlgorithm {

    private String generatedAnnotation = "#Dataset";
    private boolean fullPath = false;
    private boolean removeExtensions = true;
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public SimplePathAnnotationGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public SimplePathAnnotationGenerator(SimplePathAnnotationGenerator other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.fullPath = other.fullPath;
        this.removeExtensions = other.removeExtensions;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
            FolderData inputData = iterationStep.getInputData(getFirstInputSlot(), FolderData.class, progressInfo);
            boolean removeThisExtension = removeExtensions && Files.isRegularFile(inputData.toPath());

            String annotationValue;
            if (removeThisExtension && fullPath) {
                String fileName = inputData.toPath().getFileName().toString();
                fileName = removeExtension(fileName);
                annotationValue = inputData.toPath().getParent().resolve(fileName).toString();
            } else {
                if (fullPath) {
                    annotationValue = inputData.getPath();
                } else {
                    annotationValue = inputData.toPath().getFileName().toString();
                }
                if (removeThisExtension) {
                    annotationValue = removeExtension(annotationValue);
                }
            }

            iterationStep.addMergedTextAnnotation(new JIPipeTextAnnotation(generatedAnnotation, annotationValue), annotationMergeStrategy);
            iterationStep.addOutputData(getFirstOutputSlot(), inputData, progressInfo);
        }
    }

    private String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0)
            return fileName;
        else
            return fileName.substring(0, dotIndex);
    }

    @SetJIPipeDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each path")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with full path", description = "If true, the full path is put into the annotation. Otherwise, only the file or folder name is stored.")
    @JIPipeParameter("full-path")
    public boolean isFullPath() {
        return fullPath;
    }

    @JIPipeParameter("full-path")
    public void setFullPath(boolean fullPath) {
        this.fullPath = fullPath;
    }

    @SetJIPipeDocumentation(name = "Remove file extensions", description = "If a path is a file, remove its extension. The extension is the substring starting with the last dot. " +
            "Unix dot-files (that start with a dot) are ignored. Ignores files that have no extension.")
    @JIPipeParameter("remove-extensions")
    public boolean isRemoveExtensions() {
        return removeExtensions;
    }

    @JIPipeParameter("remove-extensions")
    public void setRemoveExtensions(boolean removeExtensions) {
        this.removeExtensions = removeExtensions;
    }

    @SetJIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if an annotation already exists.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
