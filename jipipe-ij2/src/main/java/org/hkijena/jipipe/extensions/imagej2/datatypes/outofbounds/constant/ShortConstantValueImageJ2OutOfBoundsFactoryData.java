package org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds.constant;

import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds.ImageJ2OutOfBoundsFactoryData;

import java.nio.file.Path;

@JIPipeDocumentation(name = "IJ2 Constant Short Out Of Bounds factory", description = "Sets the values outside the image border to a constant value.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class ShortConstantValueImageJ2OutOfBoundsFactoryData extends ImageJ2OutOfBoundsFactoryData {

    private short value = 0;

    public ShortConstantValueImageJ2OutOfBoundsFactoryData() {

    }

    public ShortConstantValueImageJ2OutOfBoundsFactoryData(OutOfBoundsConstantValueFactory<?,?> factory) {
        this.value = (short)((RealType)factory.getValue()).getRealDouble();
    }

    public ShortConstantValueImageJ2OutOfBoundsFactoryData(ShortConstantValueImageJ2OutOfBoundsFactoryData other) {
        this.value = other.value;
    }

    @Override
    public OutOfBoundsFactory<?, ?> createFactory() {
        return new OutOfBoundsConstantValueFactory(new ShortType(value));
    }

    public static ShortConstantValueImageJ2OutOfBoundsFactoryData importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        return (ShortConstantValueImageJ2OutOfBoundsFactoryData) ImageJ2OutOfBoundsFactoryData.importFrom(storagePath, progressInfo);
    }

    @Override
    public String toString() {
        return "IJ2 Constant Out Of Bounds Factory (Short " + value + ")";
    }

    @JIPipeDocumentation(name = "Constant value", description = "The value assumed to be outside the image border")
    @JIPipeParameter("value")
    public short getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(short value) {
        this.value = value;
    }
}
