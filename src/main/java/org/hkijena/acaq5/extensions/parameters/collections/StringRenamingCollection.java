package org.hkijena.acaq5.extensions.parameters.collections;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hkijena.acaq5.extensions.parameters.filters.StringRenaming;

/**
 * A collection of multiple {@link org.hkijena.acaq5.extensions.parameters.filters.StringRenaming}
 */
@JsonDeserialize(using = StringRenamingCollection.Deserializer.class)
public class StringRenamingCollection extends CollectionParameter<StringRenaming> {
    /**
     * Creates a new instance
     */
    public StringRenamingCollection() {
        super(StringRenaming.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringRenamingCollection(StringRenamingCollection other) {
        super(StringRenaming.class);
        for (StringRenaming filter : other) {
            add(new StringRenaming(filter));
        }
    }

    /**
     * Deserializes a {@link StringRenamingCollection}
     */
    public static class Deserializer extends CollectionParameter.Deserializer<StringRenaming> {
        @Override
        public Class<StringRenaming> getContentClass() {
            return StringRenaming.class;
        }

        @Override
        public CollectionParameter<StringRenaming> newInstance() {
            return new StringRenamingCollection();
        }
    }
}
