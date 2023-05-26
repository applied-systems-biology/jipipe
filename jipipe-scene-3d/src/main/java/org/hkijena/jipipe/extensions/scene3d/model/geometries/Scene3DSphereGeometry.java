package org.hkijena.jipipe.extensions.scene3d.model.geometries;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DGeometry;

import java.util.List;

public class Scene3DSphereGeometry implements Scene3DGeometry {

    private String name;
    private float radiusX = 1;
    private float radiusY = 1;
    private float radiusZ = 1;

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

    @Override
    public void toMesh(List<Scene3DMeshGeometry> target, JIPipeProgressInfo progressInfo) {

    }
}
