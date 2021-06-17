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

package org.hkijena.jipipe.extensions.deeplearning.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.deeplearning.datatypes.DeepLearningModelData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;

import java.nio.file.Path;

@JIPipeDocumentation(name = "Import Model", description = "Imports an existing Deep Learning model. " +
        "<ul>" +
        "<li>Model HDF5: The HDF5 file of this model</li>" +
        "<li>Model JSON: The JSON representation of this model, obtained via model.to_json()</li>" +
        "<li>Model parameters: Parameters of this model in JSON format. Should at least define img_size and n_classes</li>" +
        "</ul>")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Model HDF5", autoCreate = true)
@JIPipeInputSlot(value = FileData.class, slotName = "Model parameters", autoCreate = true)
@JIPipeInputSlot(value = FileData.class, slotName = "Model JSON", autoCreate = true)
@JIPipeOutputSlot(value = DeepLearningModelData.class, slotName = "Output", autoCreate = true)
public class ImportModelAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ImportModelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportModelAlgorithm(ImportModelAlgorithm other) {
        super(other);
    }


    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path modelHDF5Path = dataBatch.getInputData("Model HDF5", FileData.class, progressInfo).toPath();
        Path modelParametersPath = dataBatch.getInputData("Model parameters", FileData.class, progressInfo).toPath();
        Path modelJSONPath = dataBatch.getInputData("Model JSON", FileData.class, progressInfo).toPath();

        dataBatch.addOutputData(getFirstOutputSlot(), new DeepLearningModelData(modelHDF5Path, modelParametersPath, modelJSONPath), progressInfo);
    }

}
