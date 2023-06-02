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

public class Scene3DLineGeometry extends AbstractJIPipeParameterCollection implements Scene3DGeometry {

    private String name;
    private Color color = Color.RED;
    private LineEndPoint start = new LineEndPoint();
    private LineEndPoint end = new LineEndPoint();

    private int smoothness = 10;

    public Scene3DLineGeometry() {
    }

    public Scene3DLineGeometry(Scene3DLineGeometry other) {
        this.name = other.name;
        this.color = other.color;
        this.start = new LineEndPoint(other.start);
        this.end = new LineEndPoint(other.end);
        this.smoothness = other.smoothness;
    }

    @Override
    public Scene3DNode duplicate() {
        return new Scene3DLineGeometry(this);
    }

    @Override
    public Scene3DMeshGeometry toMeshGeometry(JIPipeProgressInfo progressInfo) {
        float[] vertices = Scene3DUtils.generateLineVertices(start.x, start.y, start.z, start.radius,
                end.x, end.y, end.z, end.radius, smoothness);
        float[] normals = Scene3DUtils.generateUnindexedVertexNormalsFlat(vertices);
        boolean[] mask = Scene3DUtils.findUnindexedNaNNormalVertices(vertices, normals);
        vertices = Scene3DUtils.filterArray(vertices, mask, false);
        normals = Scene3DUtils.filterArray(normals, mask, false);
        Scene3DUnindexedMeshGeometry meshObject = new Scene3DUnindexedMeshGeometry(vertices, normals);
        meshObject.setColor(color);
        return meshObject;
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

    @JIPipeDocumentation(name = "Mesh name", description = "The name of the mesh")
    @JIPipeParameter("name")
    @Override
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JIPipeParameter("name")
    @Override
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JIPipeDocumentation(name = "Line point 1", description = "The start point of the line")
    @JIPipeParameter("start")
    @JsonGetter("start")
    public LineEndPoint getStart() {
        return start;
    }

    @JsonSetter("start")
    public void setStart(LineEndPoint start) {
        this.start = start;
    }

    @JIPipeDocumentation(name = "Line point 2", description = "The end point of the line")
    @JsonGetter("end")
    @JIPipeParameter("end")
    public LineEndPoint getEnd() {
        return end;
    }

    @JsonSetter("end")
    public void setEnd(LineEndPoint end) {
        this.end = end;
    }

    public static class LineEndPoint extends AbstractJIPipeParameterCollection {
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

        @JIPipeDocumentation(name = "X", description = "The X location")
        @JIPipeParameter("x")
        @JsonGetter("x")
        public float getX() {
            return x;
        }

        @JIPipeParameter("x")
        @JsonSetter("x")
        public void setX(float x) {
            this.x = x;
        }

        @JIPipeDocumentation(name = "Y", description = "The Y location")
        @JIPipeParameter("y")
        @JsonGetter("y")
        public float getY() {
            return y;
        }

        @JIPipeParameter("y")
        @JsonSetter("y")
        public void setY(float y) {
            this.y = y;
        }

        @JIPipeDocumentation(name = "Z", description = "The Z location")
        @JIPipeParameter("z")
        @JsonGetter("z")
        public float getZ() {
            return z;
        }

        @JIPipeParameter("z")
        @JsonSetter("z")
        public void setZ(float z) {
            this.z = z;
        }

        @JIPipeDocumentation(name = "Radius", description = "The radius")
        @JIPipeParameter("radius")
        @JsonGetter("radius")
        public float getRadius() {
            return radius;
        }

        @JIPipeParameter("radius")
        @JsonSetter("radius")
        public void setRadius(float radius) {
            this.radius = radius;
        }
    }
}
