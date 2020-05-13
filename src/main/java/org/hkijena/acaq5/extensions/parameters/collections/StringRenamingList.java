package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.filters.StringRenaming;

/**
 * A collection of multiple {@link org.hkijena.acaq5.extensions.parameters.filters.StringRenaming}
 */
public class StringRenamingList extends ListParameter<StringRenaming> {
    /**
     * Creates a new instance
     */
    public StringRenamingList() {
        super(StringRenaming.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringRenamingList(StringRenamingList other) {
        super(StringRenaming.class);
        for (StringRenaming filter : other) {
            add(new StringRenaming(filter));
        }
    }
}
