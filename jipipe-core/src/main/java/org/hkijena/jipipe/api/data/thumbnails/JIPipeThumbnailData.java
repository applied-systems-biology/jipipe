package org.hkijena.jipipe.api.data.thumbnails;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;

import java.awt.*;

@JIPipeDocumentation(name = "Thumbnail", description = "Thumbnail data (used internally)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Unknown storage schema (generic data)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
@JIPipeHidden
public interface JIPipeThumbnailData extends JIPipeData {

    /**
     * Creates a Swing component that represents the thumbnail
     * @param width the requested width of the swing component
     * @param height the requested height of the swing component
     * @return the component
     */
    Component renderToComponent(int width, int height);

    /**
     * Returns true if the thumbnail data has a specific size
     * @return if the thumbnail data has a specific size
     */
    boolean hasSize();

    /**
     * The width if hasSize() is true
     * Otherwise, will return 0
     * @return the width or 0
     */
    int getWidth();

    /**
     * The height if hasSize() is true
     * Otherwise, will return 0
     * @return the height or 0
     */
    int getHeight();

    @Override
    default JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return this;
    }
}
