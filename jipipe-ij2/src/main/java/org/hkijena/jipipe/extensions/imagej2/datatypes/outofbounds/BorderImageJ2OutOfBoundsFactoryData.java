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

package org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds;

import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsPeriodicFactory;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;

@SetJIPipeDocumentation(name = "IJ2 Border Out Of Bounds factory", description = "Strategy to repeat the boundary pixels.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single JSON file that stores the status information.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class BorderImageJ2OutOfBoundsFactoryData extends ImageJ2OutOfBoundsFactoryData {

    public BorderImageJ2OutOfBoundsFactoryData() {

    }

    public BorderImageJ2OutOfBoundsFactoryData(OutOfBoundsPeriodicFactory<?, ?> factory) {
    }

    public BorderImageJ2OutOfBoundsFactoryData(BorderImageJ2OutOfBoundsFactoryData other) {
    }

    public static BorderImageJ2OutOfBoundsFactoryData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return (BorderImageJ2OutOfBoundsFactoryData) ImageJ2OutOfBoundsFactoryData.importData(storage, progressInfo);
    }

    @Override
    public OutOfBoundsFactory<?, ?> createFactory() {
        return new OutOfBoundsPeriodicFactory<>();
    }

    @Override
    public String toString() {
        return "IJ2 Border Out Of Bounds Factory";
    }
}
