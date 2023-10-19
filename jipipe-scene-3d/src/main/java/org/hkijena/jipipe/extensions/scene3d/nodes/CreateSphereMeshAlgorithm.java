package org.hkijena.jipipe.extensions.scene3d.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DSphereGeometry;

@JIPipeDocumentation(name = "Create 3D sphere mesh", description = "Generates a 3D scene containing a sphere mesh at the specified location.")
@JIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class CreateSphereMeshAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final Scene3DSphereGeometry geometry;

    public CreateSphereMeshAlgorithm(JIPipeNodeInfo info) {
        super(info);
        geometry = new Scene3DSphereGeometry();
        registerSubParameter(geometry);
    }

    public CreateSphereMeshAlgorithm(CreateSphereMeshAlgorithm other) {
        super(other);
        this.geometry = new Scene3DSphereGeometry(other.geometry);
        registerSubParameter(geometry);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Scene3DData scene3DData = new Scene3DData();
        scene3DData.add(new Scene3DSphereGeometry(getGeometry()));
        iterationStep.addOutputData(getFirstOutputSlot(), scene3DData, progressInfo);
    }

    @JIPipeDocumentation(name = "Sphere parameters", description = "The following settings allow to determine the properties of the sphere")
    @JIPipeParameter("geometry")
    public Scene3DSphereGeometry getGeometry() {
        return geometry;
    }
}
