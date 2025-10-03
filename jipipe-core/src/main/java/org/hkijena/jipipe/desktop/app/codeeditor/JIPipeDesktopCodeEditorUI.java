/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.codeeditor;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;

/**
 * Cental panel for code editing
 */
public class JIPipeDesktopCodeEditorUI extends JIPipeDesktopWorkbenchPanel {
    private JIPipeDesktopCodeEditorDocument document;

    public JIPipeDesktopCodeEditorUI(JIPipeDesktopWorkbench desktopWorkbench, JIPipeDesktopCodeEditorDocument document) {
        super(desktopWorkbench);
        setDocument(document);
    }

    public JIPipeDesktopCodeEditorUI(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        initialize();
    }

    private void initialize() {

    }

    public void setDocument(JIPipeDesktopCodeEditorDocument document) {
        this.document = document;
    }

    public JIPipeDesktopCodeEditorDocument getDocument() {
        return document;
    }
}
