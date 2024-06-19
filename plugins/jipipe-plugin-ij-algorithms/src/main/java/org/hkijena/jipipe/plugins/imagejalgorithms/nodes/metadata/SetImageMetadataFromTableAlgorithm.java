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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.metadata;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Set image metadata from table", description = "Sets the image metadata (property map) from a table. Only the first row is utilized.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Metadata")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", create = true)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Metadata", description = "Table of image metadata (one row)", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
public class SetImageMetadataFromTableAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean clearBeforeWrite = false;

    public SetImageMetadataFromTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetImageMetadataFromTableAlgorithm(SetImageMetadataFromTableAlgorithm other) {
        super(other);
        this.clearBeforeWrite = other.clearBeforeWrite;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getDuplicateImage();
        ResultsTableData metadata = iterationStep.getInputData("Metadata", ResultsTableData.class, progressInfo);

        if (metadata.getRowCount() > 0) {
            Map<String, String> properties = clearBeforeWrite ? new HashMap<>() : ImageJUtils.getImageProperties(imagePlus);
            for (String columnName : metadata.getColumnNames()) {
                properties.put(columnName, metadata.getValueAsString(0, columnName));
            }
            ImageJUtils.setImageProperties(imagePlus, properties);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(imagePlus), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Clear properties before write", description = "If enabled, all existing ROI properties are deleted before writing the new properties")
    @JIPipeParameter("clear-before-write")
    public boolean isClearBeforeWrite() {
        return clearBeforeWrite;
    }

    @JIPipeParameter("clear-before-write")
    public void setClearBeforeWrite(boolean clearBeforeWrite) {
        this.clearBeforeWrite = clearBeforeWrite;
    }
}
