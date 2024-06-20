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

package org.hkijena.jipipe.plugins.imagej2.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagej2.datatypes.shapes.EmptyImageJ2ShapeData;
import org.hkijena.jipipe.plugins.imagej2.datatypes.shapes.ImageJ2ShapeData;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataInfoRef;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeDataParameterSettings;
import org.hkijena.jipipe.utils.classfilters.NonGenericClassFilter;

@SetJIPipeDocumentation(name = "Create shape", description = "Defines an ImageJ2 shape")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = ImageJ2ShapeData.class, name = "Shape", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(getFirstOutputSlot(), shapeData.duplicate(progressInfo), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Shape type", description = "The shape that should be generated")
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

    @SetJIPipeDocumentation(name = "Shape parameters", description = "Use following settings to define the parameters of this shape")
    @JIPipeParameter(value = "shape-parameters")
    public ImageJ2ShapeData getShapeData() {
        return shapeData;
    }
}
