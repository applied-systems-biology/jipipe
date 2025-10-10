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
import net.imglib2.type.numeric.integer.LongType;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.documentation.ConfigureJIPipeDataCrate;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagej2.datatypes.outofbounds.ImageJ2OutOfBoundsFactoryData;
import org.hkijena.jipipe.plugins.imagej2.datatypes.shapes.ImageJ2ShapeData;

@SetJIPipeDocumentation(name = "IJ2 Constant Long Out Of Bounds factory", description = "Sets the values outside the image border to a constant value.")
@ConfigureJIPipeDataCrate(entities = {}, inherits = ImageJ2OutOfBoundsFactoryData.class)
public class LongConstantValueImageJ2OutOfBoundsFactoryData extends ImageJ2OutOfBoundsFactoryData {

    private long value = 0;

    public LongConstantValueImageJ2OutOfBoundsFactoryData() {

    }

    public LongConstantValueImageJ2OutOfBoundsFactoryData(OutOfBoundsConstantValueFactory<?, ?> factory) {
        this.value = (long) ((RealType) factory.getValue()).getRealDouble();
    }

    public LongConstantValueImageJ2OutOfBoundsFactoryData(LongConstantValueImageJ2OutOfBoundsFactoryData other) {
        this.value = other.value;
    }

    public static LongConstantValueImageJ2OutOfBoundsFactoryData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (LongConstantValueImageJ2OutOfBoundsFactoryData) ImageJ2OutOfBoundsFactoryData.importData(storage, progressInfo);
    }

    @Override
    public OutOfBoundsFactory<?, ?> createFactory() {
        return new OutOfBoundsConstantValueFactory(new LongType(value));
    }

    @Override
    public String toString() {
        return "IJ2 Constant Out Of Bounds Factory (Long " + value + ")";
    }

    @SetJIPipeDocumentation(name = "Constant value", description = "The value assumed to be outside the image border")
    @JIPipeParameter("value")
    public long getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(long value) {
        this.value = value;
    }
}
