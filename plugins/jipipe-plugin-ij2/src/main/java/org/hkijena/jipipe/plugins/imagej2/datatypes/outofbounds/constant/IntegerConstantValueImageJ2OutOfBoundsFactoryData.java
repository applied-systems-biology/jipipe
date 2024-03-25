/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagej2.datatypes.outofbounds.constant;

import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagej2.datatypes.outofbounds.ImageJ2OutOfBoundsFactoryData;

@SetJIPipeDocumentation(name = "IJ2 Constant Integer Out Of Bounds factory", description = "Sets the values outside the image border to a constant value.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class IntegerConstantValueImageJ2OutOfBoundsFactoryData extends ImageJ2OutOfBoundsFactoryData {

    private int value = 0;

    public IntegerConstantValueImageJ2OutOfBoundsFactoryData() {

    }

    public IntegerConstantValueImageJ2OutOfBoundsFactoryData(OutOfBoundsConstantValueFactory<?, ?> factory) {
        this.value = (int) ((RealType) factory.getValue()).getRealDouble();
    }

    public IntegerConstantValueImageJ2OutOfBoundsFactoryData(IntegerConstantValueImageJ2OutOfBoundsFactoryData other) {
        this.value = other.value;
    }

    public static IntegerConstantValueImageJ2OutOfBoundsFactoryData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (IntegerConstantValueImageJ2OutOfBoundsFactoryData) ImageJ2OutOfBoundsFactoryData.importData(storage, progressInfo);
    }

    @Override
    public OutOfBoundsFactory<?, ?> createFactory() {
        return new OutOfBoundsConstantValueFactory(new IntType(value));
    }

    @Override
    public String toString() {
        return "IJ2 Constant Out Of Bounds Factory (Integer " + value + ")";
    }

    @SetJIPipeDocumentation(name = "Constant value", description = "The value assumed to be outside the image border")
    @JIPipeParameter("value")
    public int getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(int value) {
        this.value = value;
    }
}
