package org.hkijena.jipipe.extensions.core.data;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class DefaultDataDisplayOperation implements JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        data.display(displayName, workbench, source);
    }

    @Override
    public String getId() {
        return "jipipe:show";
    }

    @Override
    public String getName() {
        return "Show";
    }

    @Override
    public String getDescription() {
        return "The default operation as defined by the data type";
    }

    @Override
    public int getOrder() {
        return -1000;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/zoom.png");
    }
}
