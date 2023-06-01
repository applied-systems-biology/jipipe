package org.hkijena.jipipe.extensions.scene3d.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DSphereGeometry;

import java.awt.*;

@JIPipeDocumentation(name = "Create 3D sphere mesh", description = "Generates a 3D scene containing a sphere mesh at the specified location.")
@JIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class CreateSphereMeshAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String meshName;
    private float radiusX = 1;
    private float radiusY = 1;
    private float radiusZ = 1;

    private int smoothness = 10;
    private Color meshColor = Color.RED;

    public CreateSphereMeshAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CreateSphereMeshAlgorithm(CreateSphereMeshAlgorithm other) {
        super(other);
        this.meshName = other.meshName;
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
        this.radiusZ = other.radiusZ;
        this.meshColor = other.meshColor;
        this.smoothness = other.smoothness;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Scene3DData scene3DData = new Scene3DData();
        Scene3DSphereGeometry geometry = new Scene3DSphereGeometry();
        geometry.setSmoothness(smoothness);
        geometry.setName(meshName);
        geometry.setColor(meshColor);
        geometry.setRadiusX(radiusX);
        geometry.setRadiusY(radiusY);
        geometry.setRadiusZ(radiusZ);
        scene3DData.add(geometry);
        dataBatch.addOutputData(getFirstOutputSlot(), scene3DData, progressInfo);
    }

    @JIPipeDocumentation(name = "Smoothness", description = "The higher the value, the smoother the generated sphere. Should be at least 1.")
    @JIPipeParameter("smoothness")
    public int getSmoothness() {
        return smoothness;
    }

    @JIPipeParameter("smoothness")
    public void setSmoothness(int smoothness) {
        this.smoothness = smoothness;
    }

    @JIPipeDocumentation(name = "Mesh name", description = "The name of the mesh")
    @JIPipeParameter("mesh-name")
    public String getMeshName() {
        return meshName;
    }

    @JIPipeParameter("mesh-name")
    public void setMeshName(String meshName) {
        this.meshName = meshName;
    }

    @JIPipeDocumentation(name = "Radius (X)", description = "The radius in the X axis")
    @JIPipeParameter("radius-x")
    public float getRadiusX() {
        return radiusX;
    }

    @JIPipeParameter("radius-x")
    public void setRadiusX(float radiusX) {
        this.radiusX = radiusX;
    }

    @JIPipeDocumentation(name = "Radius (Y)", description = "The radius in the Y axis")
    @JIPipeParameter("radius-y")
    public float getRadiusY() {
        return radiusY;
    }

    @JIPipeParameter("radius-y")
    public void setRadiusY(float radiusY) {
        this.radiusY = radiusY;
    }

    @JIPipeDocumentation(name = "Radius (Z)", description = "The radius in the Z axis")
    @JIPipeParameter("radius-z")
    public float getRadiusZ() {
        return radiusZ;
    }

    @JIPipeParameter("radius-z")
    public void setRadiusZ(float radiusZ) {
        this.radiusZ = radiusZ;
    }

    @JIPipeDocumentation(name = "Mesh color", description = "The color of the mesh")
    @JIPipeParameter("mesh-color")
    public Color getMeshColor() {
        return meshColor;
    }

    @JIPipeParameter("mesh-color")
    public void setMeshColor(Color meshColor) {
        this.meshColor = meshColor;
    }
}
