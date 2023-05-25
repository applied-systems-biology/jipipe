package org.hkijena.jipipe.extensions.scene3d.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

public class Scene3DSphereObject extends Scene3DObject {

    private float radiusX = 1;
    private float radiusY = 1;
    private float radiusZ = 1;

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
    public void toMesh(List<Scene3DMeshObject> target) {

    }
}
