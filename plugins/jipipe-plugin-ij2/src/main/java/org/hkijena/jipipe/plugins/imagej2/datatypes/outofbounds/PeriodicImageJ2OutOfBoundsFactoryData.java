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

package org.hkijena.jipipe.plugins.imagej2.datatypes.outofbounds;

import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsPeriodicFactory;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.documentation.ConfigureJIPipeDataCrate;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.utils.JIPipeSerializedParameterCollectionData;

@SetJIPipeDocumentation(name = "IJ2 Periodic Out Of Bounds factory", description = "Create appropriate strategies that virtually extend a image periodically.")
@ConfigureJIPipeDataCrate(entities = {}, inherits = ImageJ2OutOfBoundsFactoryData.class)
public class PeriodicImageJ2OutOfBoundsFactoryData extends ImageJ2OutOfBoundsFactoryData {

    public PeriodicImageJ2OutOfBoundsFactoryData() {

    }

    public PeriodicImageJ2OutOfBoundsFactoryData(OutOfBoundsPeriodicFactory<?, ?> factory) {
    }

    public PeriodicImageJ2OutOfBoundsFactoryData(PeriodicImageJ2OutOfBoundsFactoryData other) {
    }

    public static PeriodicImageJ2OutOfBoundsFactoryData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (PeriodicImageJ2OutOfBoundsFactoryData) ImageJ2OutOfBoundsFactoryData.importData(storage, progressInfo);
    }

    @Override
    public OutOfBoundsFactory<?, ?> createFactory() {
        return new OutOfBoundsPeriodicFactory<>();
    }

    @Override
    public String toString() {
        return "IJ2 Periodic Out Of Bounds Factory";
    }
}
