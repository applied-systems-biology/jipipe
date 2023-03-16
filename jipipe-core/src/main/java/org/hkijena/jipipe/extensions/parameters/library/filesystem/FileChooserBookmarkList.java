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
