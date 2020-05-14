package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.filters.IntegerRenaming;

/**
 * A collection of multiple {@link IntegerRenaming}
 */
public class IntegerRenamingList extends ListParameter<IntegerRenaming> {
    /**
     * Creates a new instance
     */
    public IntegerRenamingList() {
        super(IntegerRenaming.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerRenamingList(IntegerRenamingList other) {
        super(IntegerRenaming.class);
        for (IntegerRenaming filter : other) {
            add(new IntegerRenaming(filter));
        }
    }
}
