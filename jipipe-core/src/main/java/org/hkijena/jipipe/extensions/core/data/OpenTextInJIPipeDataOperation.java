package org.hkijena.jipipe.extensions.core.data;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class OpenTextInJIPipeDataOperation implements JIPipeDataDisplayOperation {

    private final String[] extensions;

    public OpenTextInJIPipeDataOperation(String... extensions) {
        this.extensions = extensions;
    }

    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public String getId() {
        return "jipipe:open-text-in-jipipe";
    }

    @Override
    public String getName() {
        return "Open in JIPipe";
    }

    @Override
    public String getDescription() {
        return "Opens the data in JIPipe";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/jipipe.png");
    }
}
