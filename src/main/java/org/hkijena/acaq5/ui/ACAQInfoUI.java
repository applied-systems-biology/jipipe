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

package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;

import java.awt.BorderLayout;

/**
 * UI that shows some introduction
 */
public class ACAQInfoUI extends ACAQProjectWorkbenchPanel {

    /**
     * Creates a new instance
     *
     * @param workbenchUI The workbench UI
     */
    public ACAQInfoUI(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader reader = new MarkdownReader(true);
        reader.setDocument(MarkdownDocument.fromPluginResource("documentation/introduction.md"));
        add(reader, BorderLayout.CENTER);
    }
}
