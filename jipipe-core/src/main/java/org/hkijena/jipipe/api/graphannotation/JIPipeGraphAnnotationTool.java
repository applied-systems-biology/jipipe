package org.hkijena.jipipe.api.graphannotation;

import javax.swing.*;

public interface JIPipeGraphAnnotationTool {

    String getName();

    String getTooltip();
    Icon getIcon();

    default int getPriority() {
        return 0;
    }
}
