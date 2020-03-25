package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.ACAQJsonExtension;

public class ExtensionContentRemovedEvent {
    private ACAQJsonExtension extension;
    private Object content;

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
