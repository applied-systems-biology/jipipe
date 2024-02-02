package org.hkijena.jipipe.api.data.thumbnails;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;

import java.awt.*;

@JIPipeDocumentation(name = "Thumbnail", description = "Thumbnail data (used internally)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Unknown storage schema (generic data)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
@JIPipeHidden
public interface JIPipeThumbnailData extends JIPipeData {
    Component renderToComponent(int width, int height);
}
