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

package org.hkijena.jipipe.ui.components;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Listener for any changes in a {@link javax.swing.text.Document}
 */
public abstract class DocumentChangeListener implements DocumentListener {

    /**
     * All-in-one event for insertion, removal, change
     *
     * @param documentEvent the document event
     */
    public abstract void changed(DocumentEvent documentEvent);

    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        changed(documentEvent);
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        changed(documentEvent);
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
        changed(documentEvent);
    }
}
