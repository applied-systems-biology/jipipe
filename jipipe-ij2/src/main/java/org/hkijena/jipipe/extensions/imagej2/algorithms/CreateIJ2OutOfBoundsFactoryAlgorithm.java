package org.hkijena.jipipe.extensions.imagej2.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds.EmptyImageJ2OutOfBoundsFactory;
import org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds.ImageJ2OutOfBoundsFactoryData;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.classfilters.NonGenericClassFilter;

@JIPipeDocumentation(name = "Create Out Of Bounds factory", description = "Defines an ImageJ2 Out Of Bounds factory")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ImageJ2OutOfBoundsFactoryData.class, slotName = "Factory", autoCreate = true)
public class CreateIJ2OutOfBoundsFactoryAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeDataInfoRef factoryType = new JIPipeDataInfoRef("ij2-out-of-bounds-factory-empty");
    private ImageJ2OutOfBoundsFactoryData factoryData = new EmptyImageJ2OutOfBoundsFactory();

    public CreateIJ2OutOfBoundsFactoryAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CreateIJ2OutOfBoundsFactoryAlgorithm(CreateIJ2OutOfBoundsFactoryAlgorithm other) {
        super(other);
        this.factoryType = new JIPipeDataInfoRef(other.factoryType);
        this.factoryData = (ImageJ2OutOfBoundsFactoryData) other.factoryData.duplicate(new JIPipeProgressInfo());
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        dataBatch.addOutputData(getFirstOutputSlot(), factoryData.duplicate(progressInfo), progressInfo);
    }

    @JIPipeDocumentation(name = "Factory type", description = "The factory that should be generated")
    @JIPipeDataParameterSettings(dataBaseClass = ImageJ2OutOfBoundsFactoryData.class, dataClassFilter = NonGenericClassFilter.class)
    @JIPipeParameter(value = "factory-type", important = true)
    public JIPipeDataInfoRef getFactoryType() {
        return factoryType;
    }

    @JIPipeParameter("factory-type")
    public void setFactoryType(JIPipeDataInfoRef factoryType) {
        if (factoryType.getInfo() != this.factoryType.getInfo()) {
            this.factoryType = factoryType;
            if (factoryType.getInfo() != null) {
                this.factoryData = (ImageJ2OutOfBoundsFactoryData) JIPipe.createData(factoryType.getInfo().getDataClass());
            } else {
                this.factoryData = new EmptyImageJ2OutOfBoundsFactory();
            }
            emitParameterStructureChangedEvent();
        }
    }

    @JIPipeDocumentation(name = "Factory parameters", description = "Use following settings to define the parameters of this Out Of Bounds factory")
    @JIPipeParameter(value = "factory-parameters")
    public ImageJ2OutOfBoundsFactoryData getFactoryData() {
        return factoryData;
    }
}
