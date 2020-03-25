package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.ACAQJsonExtension;

public class ExtensionContentAddedEvent {
    private ACAQJsonExtension extension;
    private Object content;

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
