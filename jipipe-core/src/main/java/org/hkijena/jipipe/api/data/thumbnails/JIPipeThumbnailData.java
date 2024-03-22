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

package org.hkijena.jipipe.api.data.thumbnails;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;

import java.awt.*;

@SetJIPipeDocumentation(name = "Thumbnail", description = "Thumbnail data (used internally)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Unknown storage schema (generic data)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
@LabelAsJIPipeHidden
public interface JIPipeThumbnailData extends JIPipeData {

    /**
     * Creates a Swing component that represents the thumbnail
     *
     * @param width  the requested width of the swing component
     * @param height the requested height of the swing component
     * @return the component
     */
    Component renderToComponent(int width, int height);

    /**
     * Returns true if the thumbnail data has a specific size
     *
     * @return if the thumbnail data has a specific size
     */
    boolean hasSize();

    /**
     * The width if hasSize() is true
     * Otherwise, will return 0
     *
     * @return the width or 0
     */
    int getWidth();

    /**
     * The height if hasSize() is true
     * Otherwise, will return 0
     *
     * @return the height or 0
     */
    int getHeight();

    @Override
    default JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return this;
    }
}
