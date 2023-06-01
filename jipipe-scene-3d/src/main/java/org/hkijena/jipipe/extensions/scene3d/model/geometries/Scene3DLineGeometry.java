package org.hkijena.jipipe.extensions.scene3d.model.geometries;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DGeometry;

import java.awt.*;

public class Scene3DLineGeometry implements Scene3DGeometry {

    private String name;
    private Color color = Color.RED;
    private LineEndPoint start = new LineEndPoint();
    private LineEndPoint end = new LineEndPoint();

    public Scene3DLineGeometry() {
    }

    public Scene3DLineGeometry(Scene3DLineGeometry other) {
        this.name = other.name;
        this.color = other.color;
        this.start = new LineEndPoint(other.start);
        this.end = new LineEndPoint(other.end);
    }

    @Override
    public Scene3DMeshGeometry toMeshGeometry(JIPipeProgressInfo progressInfo) {
        return null;
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

    @JsonGetter("start")
    public LineEndPoint getStart() {
        return start;
    }

    @JsonSetter("start")
    public void setStart(LineEndPoint start) {
        this.start = start;
    }

    @JsonGetter("end")
    public LineEndPoint getEnd() {
        return end;
    }

    @JsonSetter("end")
    public void setEnd(LineEndPoint end) {
        this.end = end;
    }

    public static class LineEndPoint {
        private float x;
        private float y;
        private float z;
        private float radius;

        public LineEndPoint() {
        }

        public LineEndPoint(LineEndPoint other) {
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            this.radius = other.radius;
        }

        @JsonGetter("x")
        public float getX() {
            return x;
        }

        @JsonSetter("x")
        public void setX(float x) {
            this.x = x;
        }

        @JsonGetter("y")
        public float getY() {
            return y;
        }

        @JsonSetter("y")
        public void setY(float y) {
            this.y = y;
        }

        @JsonGetter("z")
        public float getZ() {
            return z;
        }

        @JsonSetter("z")
        public void setZ(float z) {
            this.z = z;
        }

        @JsonGetter("radius")
        public float getRadius() {
            return radius;
        }

        @JsonSetter("radius")
        public void setRadius(float radius) {
            this.radius = radius;
        }
    }
}
