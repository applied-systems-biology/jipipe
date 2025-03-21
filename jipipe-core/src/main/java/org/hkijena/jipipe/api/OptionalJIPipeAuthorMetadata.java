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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

public class OptionalJIPipeAuthorMetadata extends OptionalParameter<JIPipeAuthorMetadata> {
    public OptionalJIPipeAuthorMetadata() {
        super(JIPipeAuthorMetadata.class);
        setEnabled(true);
        setContent(new JIPipeAuthorMetadata());
    }

    public OptionalJIPipeAuthorMetadata(OptionalJIPipeAuthorMetadata other) {
        super(JIPipeAuthorMetadata.class);
        setContent(new JIPipeAuthorMetadata(other.getContent()));
        setEnabled(false);
    }

    public static class List extends ListParameter<OptionalJIPipeAuthorMetadata> {
        public List() {
            super(OptionalJIPipeAuthorMetadata.class);
        }

        public List(List other) {
            super(OptionalJIPipeAuthorMetadata.class);
            for (OptionalJIPipeAuthorMetadata metadata : other) {
                add(new OptionalJIPipeAuthorMetadata(metadata));
            }
        }
    }
}
