package org.hkijena.jipipe.extensions.ij3d.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DSurface;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DUnindexedMeshGeometry;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;
import org.scijava.vecmath.Point3f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around an {@link mcib3d.geom.Object3D} that also provides additional information and methods found within {@link ij.gui.Roi}
 */
public class ROI3D {
    private Object3D object3D;

    private Map<String, String> metadata = new HashMap<>();

    private int channel;

    private int frame;

    private Color fillColor = Color.RED;

    public ROI3D() {
    }

    public ROI3D(ROI3D other) {
        this.object3D = IJ3DUtils.duplicateObject3D(other.object3D);
        copyMetadata(other);
    }

    public ROI3D(Object3D object3D) {
        this.object3D = object3D;
    }

    public void copyMetadata(ROI3D other) {
        this.metadata = new HashMap<>(other.metadata);
        this.channel = other.channel;
        this.frame = other.frame;
        this.fillColor = other.fillColor;
    }

    public Object3D getObject3D() {
        return object3D;
    }

    public void setObject3D(Object3D object3D) {
        this.object3D = object3D;
    }

    @JsonGetter("metadata")
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @JsonSetter("metadata")
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @JsonGetter("channel")
    public int getChannel() {
        return channel;
    }

    @JsonSetter("channel")
    public void setChannel(int channel) {
        this.channel = channel;
    }

    @JsonGetter("frame")
    public int getFrame() {
        return frame;
    }

    @JsonSetter("frame")
    public void setFrame(int frame) {
        this.frame = frame;
    }

    @JsonGetter("fill-color")
    public Color getFillColor() {
        return fillColor;
    }

    @JsonSetter("fill-color")
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public boolean sameChannel(int channel) {
        return this.channel <= 0 || channel <= 0 || channel == this.channel;
    }

    public boolean sameFrame(int frame) {
        return this.frame <= 0 || frame <= 0 || frame == this.frame;
    }

    public String getName() {
        return object3D.getName();
    }

    public void setName(String name) {
        object3D.setName(name);
    }

    public Scene3DUnindexedMeshGeometry toGeometry(Color overrideColor, boolean physicalSizes, boolean forceMeshLengthUnit, Quantity.LengthUnit meshLengthUnit, boolean smooth, JIPipeProgressInfo progressInfo) {

        float voxDimXY = 1;
        float voxDimZ = 1;

        if(physicalSizes) {
            voxDimXY = (float) object3D.getResXY();
            voxDimZ = (float) object3D.getResZ();
            if(forceMeshLengthUnit) {
                Quantity inputQuantity = new Quantity(1, object3D.getUnits());
                Quantity outputQuantity = inputQuantity.convertTo(meshLengthUnit.toString());
                voxDimXY *= outputQuantity.getValue();
                voxDimZ *= outputQuantity.getValue();
            }
        }

        progressInfo.log("Calculating surface ...");
        Object3DSurface object3DSurface = object3D.getObject3DSurface();

        progressInfo.log("Extracting vertices ...");
        List<Point3f> surfaceTriangles = object3DSurface.getSurfaceTrianglesPixels(smooth);

        float[] vertices = new float[surfaceTriangles.size() * 3];
        for (int i = 0; i < surfaceTriangles.size(); i++) {
            Point3f surfaceTriangle = surfaceTriangles.get(i);
            float x = surfaceTriangle.x * voxDimXY;
            float y = surfaceTriangle.y * voxDimXY;
            float z = surfaceTriangle.z * voxDimZ;
            vertices[i * 3] = x;
            vertices[i * 3 + 1] = y;
            vertices[i * 3 + 2] = z;
        }

        progressInfo.log("Calculating normals ...");
        float[] normals = Scene3DUtils.generateUnindexedVertexNormalsFlat(vertices);
        boolean[] mask = Scene3DUtils.findUnindexedNaNNormalVertices(vertices, normals);
        vertices = Scene3DUtils.filterArray(vertices, mask, false);
        normals = Scene3DUtils.filterArray(normals, mask, false);

        Scene3DUnindexedMeshGeometry meshObject = new Scene3DUnindexedMeshGeometry(vertices, normals);
        meshObject.setColor(overrideColor != null ? overrideColor : getFillColor());
        return meshObject;
    }
}
