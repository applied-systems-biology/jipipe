package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

@JIPipeDocumentation(name = "IJ2 Hypersphere Shape", description = "A Shape representing a hypersphere.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class HyperSphereImageJ2ShapeData extends ImageJ2ShapeData {

    private long radius = 5;

    public HyperSphereImageJ2ShapeData() {

    }

    public HyperSphereImageJ2ShapeData(HyperSphereImageJ2ShapeData other) {
        this.radius = other.radius;
    }

    public HyperSphereImageJ2ShapeData(HyperSphereShape shape) {
        this.radius = shape.getRadius();
    }

    public static HyperSphereImageJ2ShapeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (HyperSphereImageJ2ShapeData) ImageJ2ShapeData.importData(storage, progressInfo);
    }

    @Override
    public Shape createShape() {
        return new HyperSphereShape(radius);
    }

    @Override
    public String toString() {
        return "IJ2 Hypersphere Shape (radius=" + radius + ")";
    }

    @JIPipeDocumentation(name = "Radius")
    @JIPipeParameter("radius")
    public long getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(long radius) {
        this.radius = radius;
    }
}
