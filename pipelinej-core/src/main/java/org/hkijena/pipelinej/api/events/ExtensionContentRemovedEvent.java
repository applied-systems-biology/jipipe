/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.api.events;

import org.hkijena.pipelinej.ACAQJsonExtension;

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
