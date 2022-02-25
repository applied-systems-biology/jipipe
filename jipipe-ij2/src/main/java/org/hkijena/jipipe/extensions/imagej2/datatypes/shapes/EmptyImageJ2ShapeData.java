package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;

import java.nio.file.Path;

@JIPipeDocumentation(name = "IJ2 Empty Shape", description = "An empty shape. Please note that this shape type is not usable in IJ2 algorithms.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class EmptyImageJ2ShapeData extends ImageJ2ShapeData {

    public EmptyImageJ2ShapeData() {

    }

    public EmptyImageJ2ShapeData(EmptyImageJ2ShapeData other) {
    }

    @Override
    public Shape createShape() {
        return null;
    }

    public static EmptyImageJ2ShapeData importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        return (EmptyImageJ2ShapeData) ImageJ2ShapeData.importFrom(storagePath, progressInfo);
    }

    @Override
    public String toString() {
        return "Empty IJ2 Shape";
    }
}
