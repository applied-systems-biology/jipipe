package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.utils.JIPipeSerializedParameterCollectionData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

@JIPipeDocumentation(name = "IJ2 Shape", description = "An ImageJ2 shape generator")
@JIPipeDataStorageDocumentation(humanReadableDescription = "This is a generic data type. The storage folder is empty.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
public abstract class ImageJ2ShapeData extends JIPipeSerializedParameterCollectionData {


    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        // TODO implement
    }

    public abstract Shape createShape();
}
