package org.hkijena.jipipe.extensions.imagej2.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagej2.datatypes.shapes.EmptyImageJ2ShapeData;
import org.hkijena.jipipe.extensions.imagej2.datatypes.shapes.ImageJ2ShapeData;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.classfilters.NonGenericClassFilter;

@JIPipeDocumentation(name = "Create shape", description = "Defines an ImageJ2 shape")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ImageJ2ShapeData.class, slotName = "Shape", autoCreate = true)
public class CreateIJ2ShapeAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef shapeType = new JIPipeDataInfoRef("ij2-shape-empty");
    private ImageJ2ShapeData shapeData = new EmptyImageJ2ShapeData();

    public CreateIJ2ShapeAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CreateIJ2ShapeAlgorithm(CreateIJ2ShapeAlgorithm other) {
        super(other);
        this.shapeType = new JIPipeDataInfoRef(other.shapeType);
        this.shapeData = (ImageJ2ShapeData) other.shapeData.duplicate(new JIPipeProgressInfo());
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        dataBatch.addOutputData(getFirstOutputSlot(), shapeData.duplicate(progressInfo), progressInfo);
    }

    @JIPipeDocumentation(name = "Shape type", description = "The shape that should be generated")
    @JIPipeDataParameterSettings(dataBaseClass = ImageJ2ShapeData.class, dataClassFilter = NonGenericClassFilter.class)
    @JIPipeParameter(value = "shape-type", important = true)
    public JIPipeDataInfoRef getShapeType() {
        return shapeType;
    }

    @JIPipeParameter("shape-type")
    public void setShapeType(JIPipeDataInfoRef shapeType) {
        if (shapeType.getInfo() != this.shapeType.getInfo()) {
            this.shapeType = shapeType;
            if (shapeType.getInfo() != null) {
                this.shapeData = (ImageJ2ShapeData) JIPipe.createData(shapeType.getInfo().getDataClass());
            } else {
                this.shapeData = new EmptyImageJ2ShapeData();
            }
            emitParameterStructureChangedEvent();
        }
    }

    @JIPipeDocumentation(name = "Shape parameters", description = "Use following settings to define the parameters of this shape")
    @JIPipeParameter(value = "shape-parameters")
    public ImageJ2ShapeData getShapeData() {
        return shapeData;
    }
}
