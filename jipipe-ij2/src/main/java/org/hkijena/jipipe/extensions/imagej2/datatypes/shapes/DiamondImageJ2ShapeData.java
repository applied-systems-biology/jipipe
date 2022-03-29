package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

@JIPipeDocumentation(name = "IJ2 Diamond Shape")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class DiamondImageJ2ShapeData extends ImageJ2ShapeData {

    private long radius = 5;

    public DiamondImageJ2ShapeData() {

    }

    public DiamondImageJ2ShapeData(DiamondImageJ2ShapeData other) {
        this.radius = other.radius;
    }

    public DiamondImageJ2ShapeData(DiamondShape shape) {
        this.radius = shape.getRadius();
    }

    public static DiamondImageJ2ShapeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (DiamondImageJ2ShapeData) ImageJ2ShapeData.importData(storage, progressInfo);
    }

    @Override
    public Shape createShape() {
        return new DiamondShape(radius);
    }

    @Override
    public String toString() {
        return "IJ2 Diamond Shape (radius=" + radius + ")";
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
