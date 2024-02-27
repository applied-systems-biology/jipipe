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

package org.hkijena.jipipe.extensions.parameters.library.filesystem;

import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

public class FileChooserBookmarkList extends ListParameter<FileChooserBookmark> {
    /**
     *
     */
    public FileChooserBookmarkList() {
        super(FileChooserBookmark.class);
    }

    public FileChooserBookmarkList(FileChooserBookmarkList other) {
        super(FileChooserBookmark.class);
        for (FileChooserBookmark fileChooserBookmark : other) {
            add(new FileChooserBookmark(fileChooserBookmark));
        }
    }

}
