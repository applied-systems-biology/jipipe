package org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds;

import net.imglib2.outofbounds.OutOfBoundsFactory;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.JIPipeSerializedParameterCollectionData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

@JIPipeDocumentation(name = "IJ2 Out Of Bounds factory", description = "An ImageJ2 generator for values outside the image bounds")
@JIPipeDataStorageDocumentation(humanReadableDescription = "This is a generic data type. The storage folder is empty.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
public abstract class ImageJ2OutOfBoundsFactoryData extends JIPipeSerializedParameterCollectionData {


    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        // TODO implement
    }

    public abstract OutOfBoundsFactory<?, ?> createFactory();
}
