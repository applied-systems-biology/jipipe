package org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds;

import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsPeriodicFactory;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.nio.file.Path;

@JIPipeDocumentation(name = "IJ2 Periodic Out Of Bounds factory", description = "Create appropriate strategies that virtually extend a image periodically.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class PeriodicImageJ2OutOfBoundsFactoryData extends ImageJ2OutOfBoundsFactoryData {

    public PeriodicImageJ2OutOfBoundsFactoryData() {

    }

    public PeriodicImageJ2OutOfBoundsFactoryData(OutOfBoundsPeriodicFactory<?,?> factory) {
    }

    public PeriodicImageJ2OutOfBoundsFactoryData(PeriodicImageJ2OutOfBoundsFactoryData other) {
    }

    @Override
    public OutOfBoundsFactory<?, ?> createFactory() {
        return new OutOfBoundsPeriodicFactory<>();
    }

    public static PeriodicImageJ2OutOfBoundsFactoryData importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        return (PeriodicImageJ2OutOfBoundsFactoryData) ImageJ2OutOfBoundsFactoryData.importFrom(storagePath, progressInfo);
    }

    @Override
    public String toString() {
        return "IJ2 Periodic Out Of Bounds Factory";
    }
}
