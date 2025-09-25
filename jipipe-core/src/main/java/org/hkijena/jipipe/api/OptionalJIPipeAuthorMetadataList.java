package org.hkijena.jipipe.api;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class OptionalJIPipeAuthorMetadataList extends JIPipeListParameter<OptionalJIPipeAuthorMetadata> {
    public OptionalJIPipeAuthorMetadataList() {
        super(OptionalJIPipeAuthorMetadata.class);
    }

    public OptionalJIPipeAuthorMetadataList(OptionalJIPipeAuthorMetadataList other) {
        super(OptionalJIPipeAuthorMetadata.class);
        for (OptionalJIPipeAuthorMetadata metadata : other) {
            add(new OptionalJIPipeAuthorMetadata(metadata));
        }
    }
}
