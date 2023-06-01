package org.hkijena.jipipe.extensions.scene3d.model.geometries;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DGeometry;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;

import java.awt.*;

public class Scene3DSphereGeometry implements Scene3DGeometry {

    private String name;
    private float radiusX = 1;
    private float radiusY = 1;
    private float radiusZ = 1;
    private Color color = Color.RED;
    private int smoothness = 10;

    public Scene3DSphereGeometry() {
    }

    public Scene3DSphereGeometry(Scene3DSphereGeometry other) {
        this.name = other.name;
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
        this.radiusZ = other.radiusZ;
        this.color = other.color;
    }

    @Override
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @Override
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("radius-x")
    public float getRadiusX() {
        return radiusX;
    }

    @JsonSetter("radius-x")
    public void setRadiusX(float radiusX) {
        this.radiusX = radiusX;
    }

    @JsonGetter("radius-y")
    public float getRadiusY() {
        return radiusY;
    }

    @JsonSetter("radius-y")
    public void setRadiusY(float radiusY) {
        this.radiusY = radiusY;
    }

    @JsonGetter("radius-z")
    public float getRadiusZ() {
        return radiusZ;
    }

    @JsonSetter("radius-z")
    public void setRadiusZ(float radiusZ) {
        this.radiusZ = radiusZ;
    }

    @JsonGetter("color")
    @Override
    public Color getColor() {
        return color;
    }

    @JsonSetter("color")
    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    public int getSmoothness() {
        return smoothness;
    }

    public void setSmoothness(int smoothness) {
        this.smoothness = smoothness;
    }

    @Override
    public Scene3DMeshGeometry toMeshGeometry(JIPipeProgressInfo progressInfo) {
        float[] vertices = Scene3DUtils.generateUVSphereVertices(radiusX, radiusY, radiusZ, smoothness);
        float[] normals = Scene3DUtils.generateUnindexedVertexNormalsFlat(vertices);
        boolean[] mask = Scene3DUtils.findUnindexedNaNNormalVertices(vertices, normals);
        vertices = Scene3DUtils.filterArray(vertices, mask, false);
        normals = Scene3DUtils.filterArray(normals, mask, false);
        Scene3DUnindexedMeshGeometry meshObject = new Scene3DUnindexedMeshGeometry(vertices, normals);
        meshObject.setColor(color);
        return meshObject;
    }
}
