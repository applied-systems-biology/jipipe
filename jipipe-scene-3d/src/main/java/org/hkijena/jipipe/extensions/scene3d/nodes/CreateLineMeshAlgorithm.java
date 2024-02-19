package org.hkijena.jipipe.extensions.scene3d.nodes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DLineGeometry;

@SetJIPipeDocumentation(name = "Create 3D line mesh", description = "Generates a 3D scene containing a line mesh.")
@AddJIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", create = true)
@DefineJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class CreateLineMeshAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final Scene3DLineGeometry geometry;

    public CreateLineMeshAlgorithm(JIPipeNodeInfo info) {
        super(info);
        geometry = new Scene3DLineGeometry();
        registerSubParameter(geometry);
    }

    public CreateLineMeshAlgorithm(CreateLineMeshAlgorithm other) {
        super(other);
        this.geometry = new Scene3DLineGeometry(other.geometry);
        registerSubParameter(geometry);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Scene3DData scene3DData = new Scene3DData();
        scene3DData.add(new Scene3DLineGeometry(getGeometry()));
        iterationStep.addOutputData(getFirstOutputSlot(), scene3DData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Line parameters", description = "The following settings allow to determine the properties of the line")
    @JIPipeParameter("geometry")
    public Scene3DLineGeometry getGeometry() {
        return geometry;
    }
}
