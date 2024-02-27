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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;

/**
 * A content panel with access to a {@link JIPipeWorkbench}
 */
public class FlexContentWorkbenchPanel extends FlexContentPanel implements JIPipeWorkbenchAccess {

    private final JIPipeWorkbench workbench;

    public FlexContentWorkbenchPanel(JIPipeWorkbench workbench) {
        this(workbench, NONE);
    }

    public FlexContentWorkbenchPanel(JIPipeWorkbench workbench, int flags) {
        super(flags);
        this.workbench = workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
