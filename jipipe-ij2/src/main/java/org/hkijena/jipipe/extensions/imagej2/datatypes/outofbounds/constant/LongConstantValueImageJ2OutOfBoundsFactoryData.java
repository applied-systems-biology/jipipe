package org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds.constant;

import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds.ImageJ2OutOfBoundsFactoryData;

import java.nio.file.Path;

@JIPipeDocumentation(name = "IJ2 Constant Long Out Of Bounds factory", description = "Sets the values outside the image border to a constant value.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class LongConstantValueImageJ2OutOfBoundsFactoryData extends ImageJ2OutOfBoundsFactoryData {

    private long value = 0;

    public LongConstantValueImageJ2OutOfBoundsFactoryData() {

    }

    public LongConstantValueImageJ2OutOfBoundsFactoryData(OutOfBoundsConstantValueFactory<?,?> factory) {
        this.value = (long)((RealType)factory.getValue()).getRealDouble();
    }

    public LongConstantValueImageJ2OutOfBoundsFactoryData(LongConstantValueImageJ2OutOfBoundsFactoryData other) {
        this.value = other.value;
    }

    @Override
    public OutOfBoundsFactory<?, ?> createFactory() {
        return new OutOfBoundsConstantValueFactory(new LongType(value));
    }

    public static LongConstantValueImageJ2OutOfBoundsFactoryData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (LongConstantValueImageJ2OutOfBoundsFactoryData) ImageJ2OutOfBoundsFactoryData.importData(storage, progressInfo);
    }

    @Override
    public String toString() {
        return "IJ2 Constant Out Of Bounds Factory (Long " + value + ")";
    }

    @JIPipeDocumentation(name = "Constant value", description = "The value assumed to be outside the image border")
    @JIPipeParameter("value")
    public long getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(long value) {
        this.value = value;
    }
}
