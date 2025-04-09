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

package org.hkijena.jipipe.api.data.utils;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;

import java.lang.ref.WeakReference;

/**
 * A helper data type that weakly references other data
 */
@SetJIPipeDocumentation(name = "Weak data reference", description = "References other data (weakly).")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Unknown storage schema (generic data)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
@LabelAsJIPipeHidden
public class JIPipeWeakDataReferenceData implements JIPipeData {

    private final WeakReference<JIPipeData> dataReference;

    public JIPipeWeakDataReferenceData(JIPipeData target) {
        dataReference = new WeakReference<>(target);
    }

    public static JIPipeWeakDataReferenceData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new JIPipeWeakDataReferenceData(null);
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        JIPipeData data = dataReference.get();
        if (data != null) {
            data.exportData(storage, name, forceName, progressInfo);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeWeakDataReferenceData(dataReference.get());
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        JIPipeData data = dataReference.get();
        if (data != null) {
            return data.createThumbnail(width, height, progressInfo);
        } else {
            return null;
        }
    }

    public WeakReference<JIPipeData> getDataReference() {
        return dataReference;
    }
}
