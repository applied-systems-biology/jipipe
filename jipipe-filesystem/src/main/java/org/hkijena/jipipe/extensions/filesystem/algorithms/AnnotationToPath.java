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
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Filters input files
 */
@JIPipeDocumentation(name = "Annotation to path", description = "Converts an annotation column of arbitrary data to a path. " +
        "If the specified annotation is not present, an empty path is generated.")
@JIPipeOrganization(menuPath = "Convert", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
public class AnnotationToPath extends JIPipeSimpleIteratingAlgorithm {

    private StringPredicate annotationColumn = new StringPredicate(StringPredicate.Mode.Equals, "Dataset", false);

    /**
     * Instantiates the algorithm
     *
     * @param info Algorithm info
     */
    public AnnotationToPath(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public AnnotationToPath(AnnotationToPath other) {
        super(other);
        this.annotationColumn = new StringPredicate(other.annotationColumn);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        String matchedColumn = dataBatch.getAnnotations().keySet().stream().filter(annotationColumn).findFirst().orElse(null);
        if (StringUtils.isNullOrEmpty(matchedColumn)) {
            dataBatch.addOutputData(getFirstOutputSlot(), new PathData(Paths.get("")));
            return;
        }
        JIPipeAnnotation annotation = dataBatch.getAnnotationOfType(matchedColumn);
        if (annotation == null) {
            dataBatch.addOutputData(getFirstOutputSlot(), new PathData(Paths.get("")));
            return;
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new PathData(Paths.get(StringUtils.orElse(annotation.getValue(), ""))));
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "Annotation column", description = "The annotation column that contains the paths")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    @JIPipeParameter("annotation-column")
    public StringPredicate getAnnotationColumn() {
        return annotationColumn;
    }

    @JIPipeParameter("annotation-column")
    public void setAnnotationColumn(StringPredicate annotationColumn) {
        this.annotationColumn = annotationColumn;
    }
}
