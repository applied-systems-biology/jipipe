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

package org.hkijena.jipipe.plugins.cellpose.algorithms.cp2;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.PretrainedCellpose2SegmentationModel;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp2.PretrainedCellpose2SegmentationModelList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Pretrained Cellpose 2.x model", description = "Imports one or a selection of pretrained Cellpose 2.x models")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = CellposeModelData.class, name = "Output", create = true)
public class ImportPretrainedCellpose2ModelAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private PretrainedCellpose2SegmentationModelList models = new PretrainedCellpose2SegmentationModelList();
    private OptionalTextAnnotationNameParameter modelNameAnnotation = new OptionalTextAnnotationNameParameter("Model", true);

    public ImportPretrainedCellpose2ModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
        models.add(PretrainedCellpose2SegmentationModel.cyto2);
    }

    public ImportPretrainedCellpose2ModelAlgorithm(ImportPretrainedCellpose2ModelAlgorithm other) {
        super(other);
        this.models = new PretrainedCellpose2SegmentationModelList(other.models);
        this.modelNameAnnotation = new OptionalTextAnnotationNameParameter(other.modelNameAnnotation);
    }

    @SetJIPipeDocumentation(name = "Models", description = "The list of models to be imported")
    @JIPipeParameter(value = "models", important = true)
    public PretrainedCellpose2SegmentationModelList getModels() {
        return models;
    }

    @JIPipeParameter("models")
    public void setModels(PretrainedCellpose2SegmentationModelList models) {
        this.models = models;
    }

    @SetJIPipeDocumentation(name = "Annotate with model name", description = "If enabled, annotate with the name/id of the model")
    @JIPipeParameter("model-name-annotation")
    public OptionalTextAnnotationNameParameter getModelNameAnnotation() {
        return modelNameAnnotation;
    }

    @JIPipeParameter("model-name-annotation")
    public void setModelNameAnnotation(OptionalTextAnnotationNameParameter modelNameAnnotation) {
        this.modelNameAnnotation = modelNameAnnotation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (PretrainedCellpose2SegmentationModel model : models) {
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            modelNameAnnotation.addAnnotationIfEnabled(annotationList, model.getId());
            iterationStep.addOutputData(getFirstOutputSlot(), new CellposeModelData(model.getId()), annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }
}
