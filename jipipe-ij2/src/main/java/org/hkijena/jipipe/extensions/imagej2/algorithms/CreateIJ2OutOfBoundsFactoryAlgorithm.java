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

package org.hkijena.jipipe.extensions.imagej2.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds.EmptyImageJ2OutOfBoundsFactory;
import org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds.ImageJ2OutOfBoundsFactoryData;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.classfilters.NonGenericClassFilter;

@SetJIPipeDocumentation(name = "Create Out Of Bounds factory", description = "Defines an ImageJ2 Out Of Bounds factory")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = ImageJ2OutOfBoundsFactoryData.class, slotName = "Factory", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(getFirstOutputSlot(), factoryData.duplicate(progressInfo), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Factory type", description = "The factory that should be generated")
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

    @SetJIPipeDocumentation(name = "Factory parameters", description = "Use following settings to define the parameters of this Out Of Bounds factory")
    @JIPipeParameter(value = "factory-parameters")
    public ImageJ2OutOfBoundsFactoryData getFactoryData() {
        return factoryData;
    }
}
