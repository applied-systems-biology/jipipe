package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.convert;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.editors.JIPipeDataParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Convert table to image", description = "Converts a table of pixel information into an image")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class TableToImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef outputImageType = new JIPipeDataInfoRef();
    private final JIPipeDynamicParameterCollection columnAssignment;

    public TableToImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.columnAssignment = new JIPipeDynamicParameterCollection(false);
    }

    public TableToImageAlgorithm(TableToImageAlgorithm other) {
        super(other);
        this.columnAssignment = new JIPipeDynamicParameterCollection(other.columnAssignment);
        this.outputImageType = other.outputImageType;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }

    @JIPipeDocumentation(name = "Output image type", description = "The image type that is generated.")
    @JIPipeParameter("output-image-type")
    @JIPipeDataParameterSettings(dataBaseClass = ImagePlusData.class)
    public JIPipeDataInfoRef getOutputImageType() {
        return outputImageType;
    }

    @JIPipeParameter("output-image-type")
    public void setOutputImageType(JIPipeDataInfoRef outputImageType) {
        if(outputImageType.getInfo() != this.outputImageType.getInfo()) {
            this.outputImageType = outputImageType;
            updateColumnAssignment();
        }
    }

    private void updateColumnAssignment() {

    }

    public JIPipeDynamicParameterCollection getColumnAssignment() {
        return columnAssignment;
    }
}
