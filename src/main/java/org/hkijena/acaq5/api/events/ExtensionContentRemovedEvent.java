package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.ACAQJsonExtension;

/**
 * Generated when content is removed from an {@link ACAQJsonExtension}
 */
public class ExtensionContentRemovedEvent {
    private ACAQJsonExtension extension;
    private Object content;

    /**
     * @param extension event source
     * @param content   removed content
     */
    public ExtensionContentRemovedEvent(ACAQJsonExtension extension, Object content) {
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
