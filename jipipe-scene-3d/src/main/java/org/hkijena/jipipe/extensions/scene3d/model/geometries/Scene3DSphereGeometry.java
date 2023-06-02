package org.hkijena.jipipe.extensions.scene3d.model.geometries;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DGeometry;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DNode;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;

import java.awt.*;

public class Scene3DSphereGeometry extends AbstractJIPipeParameterCollection implements Scene3DGeometry {

    private String name;
    private float radiusX = 1;
    private float radiusY = 1;
    private float radiusZ = 1;

    private float centerX = 0;
    private float centerY = 0;
    private float centerZ = 0;
    private Color color = Color.RED;
    private int smoothness = 10;

    public Scene3DSphereGeometry() {
    }

    public Scene3DSphereGeometry(Scene3DSphereGeometry other) {
        this.name = other.name;
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
        this.radiusZ = other.radiusZ;
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.centerZ = other.centerZ;
        this.color = other.color;
        this.smoothness = other.smoothness;
    }

    @Override
    public Scene3DNode duplicate() {
        return new Scene3DSphereGeometry(this);
    }

    @Override
    @JIPipeDocumentation(name = "Mesh name", description = "The name of the mesh")
    @JIPipeParameter("name")
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @Override
    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JIPipeDocumentation(name = "Radius (X)", description = "The radius in the X axis")
    @JIPipeParameter("radius-x")
    @JsonGetter("radius-x")
    public float getRadiusX() {
        return radiusX;
    }

    @JIPipeParameter("radius-x")
    @JsonSetter("radius-x")
    public void setRadiusX(float radiusX) {
        this.radiusX = radiusX;
    }

    @JIPipeDocumentation(name = "Radius (Y)", description = "The radius in the Y axis")
    @JIPipeParameter("radius-y")
    @JsonGetter("radius-y")
    public float getRadiusY() {
        return radiusY;
    }

    @JIPipeParameter("radius-y")
    @JsonSetter("radius-y")
    public void setRadiusY(float radiusY) {
        this.radiusY = radiusY;
    }

    @JIPipeDocumentation(name = "Radius (Z)", description = "The radius in the Z axis")
    @JIPipeParameter("radius-z")
    @JsonGetter("radius-z")
    public float getRadiusZ() {
        return radiusZ;
    }

    @JIPipeParameter("radius-z")
    @JsonSetter("radius-z")
    public void setRadiusZ(float radiusZ) {
        this.radiusZ = radiusZ;
    }

    @JIPipeDocumentation(name = "Center (X)", description = "The X coordinate of the center")
    @JIPipeParameter("center-x")
    @JsonGetter("center-x")
    public float getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    @JsonSetter("center-x")
    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    @JIPipeDocumentation(name = "Center (Y)", description = "The Y coordinate of the center")
    @JIPipeParameter("center-y")
    @JsonGetter("center-y")
    public float getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    @JsonSetter("center-y")
    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    @JIPipeDocumentation(name = "Center (Z)", description = "The Z coordinate of the center")
    @JIPipeParameter("center-z")
    @JsonGetter("center-z")
    public float getCenterZ() {
        return centerZ;
    }

    @JIPipeParameter("center-z")
    @JsonSetter("center-z")
    public void setCenterZ(float centerZ) {
        this.centerZ = centerZ;
    }

    @JIPipeDocumentation(name = "Color", description = "The color of the mesh")
    @JIPipeParameter("color")
    @JsonGetter("color")
    @Override
    public Color getColor() {
        return color;
    }

    @JIPipeParameter("color")
    @JsonSetter("color")
    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    @JIPipeDocumentation(name = "Smoothness", description = "The higher the value, the smoother the generated sphere. Should be at least 1.")
    @JsonGetter("smoothness")
    public int getSmoothness() {
        return smoothness;
    }

    @JsonSetter("smoothness")
    public void setSmoothness(int smoothness) {
        this.smoothness = smoothness;
    }

    @Override
    public Scene3DMeshGeometry toMeshGeometry(JIPipeProgressInfo progressInfo) {
        float[] vertices = Scene3DUtils.generateUVSphereVertices(centerX, centerY, centerZ, radiusX, radiusY, radiusZ, smoothness);
        float[] normals = Scene3DUtils.generateUnindexedVertexNormalsFlat(vertices);
        boolean[] mask = Scene3DUtils.findUnindexedNaNNormalVertices(vertices, normals);
        vertices = Scene3DUtils.filterArray(vertices, mask, false);
        normals = Scene3DUtils.filterArray(normals, mask, false);
        Scene3DUnindexedMeshGeometry meshObject = new Scene3DUnindexedMeshGeometry(vertices, normals);
        meshObject.setColor(color);
        return meshObject;
    }
}
