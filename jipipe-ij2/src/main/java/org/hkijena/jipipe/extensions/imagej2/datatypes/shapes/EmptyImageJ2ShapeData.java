package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;

@SetJIPipeDocumentation(name = "IJ2 Empty Shape", description = "An empty shape. Please note that this shape type is not usable in IJ2 algorithms.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class EmptyImageJ2ShapeData extends ImageJ2ShapeData {

    public EmptyImageJ2ShapeData() {

    }

    public EmptyImageJ2ShapeData(EmptyImageJ2ShapeData other) {
    }

    public static EmptyImageJ2ShapeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (EmptyImageJ2ShapeData) ImageJ2ShapeData.importData(storage, progressInfo);
    }

    @Override
    public Shape createShape() {
        return null;
    }

    @Override
    public String toString() {
        return "Empty IJ2 Shape";
    }
}
