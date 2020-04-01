package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.ACAQJsonExtension;

/**
 * Generated when content is added to an {@link ACAQJsonExtension}
 */
public class ExtensionContentAddedEvent {
    private ACAQJsonExtension extension;
    private Object content;

    /**
     * @param extension event source
     * @param content   the new content
     */
    public ExtensionContentAddedEvent(ACAQJsonExtension extension, Object content) {
        this.extension = extension;
        this.content = content;
    }

    public ACAQJsonExtension getExtension() {
        return extension;
    }

    public Object getContent() {
        return content;
    }
}
