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

package org.hkijena.jipipe.extensions.ijweka.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;

@SetJIPipeDocumentation(name = "Import Weka model", description = "Imports a Trainable Weka Segmentation model from a *.model/*.arff file")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, slotName = "Model file", description = "The model file in *.model format", create = true)
@AddJIPipeInputSlot(value = FileData.class, slotName = "Data file", description = "The data file in *.arff format", create = true, optional = true)
@AddJIPipeOutputSlot(value = WekaModelData.class, slotName = "Output", description = "The model", create = true)
public class ImportWekaModelFromFileAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean processing3D;

    public ImportWekaModelFromFileAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportWekaModelFromFileAlgorithm(ImportWekaModelFromFileAlgorithm other) {
        super(other);
        this.processing3D = other.processing3D;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FileData modelFileData = iterationStep.getInputData("Model file", FileData.class, progressInfo);
        FileData dataFileData = iterationStep.getInputData("Data file", FileData.class, progressInfo);
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            WekaSegmentation segmentation = new WekaSegmentation(processing3D);
            if (dataFileData != null) {
                segmentation.loadTrainingData(dataFileData.getPath());
            }
            segmentation.loadClassifier(modelFileData.getPath());
            WekaModelData modelData = new WekaModelData(segmentation);
            iterationStep.addOutputData(getFirstOutputSlot(), modelData, progressInfo);
        }

    }

    @SetJIPipeDocumentation(name = "Is processing 3D data", description = "If enabled, indicates that the model is a 3D model. <strong>Please ensure to set the correct value; otherwise " +
            "the Weka model will fail to import or behave in unexpected ways</strong>")
    @JIPipeParameter(value = "is-processing-3d", important = true)
    public boolean isProcessing3D() {
        return processing3D;
    }

    @JIPipeParameter("is-processing-3d")
    public void setProcessing3D(boolean processing3D) {
        this.processing3D = processing3D;
    }
}
