package org.hkijena.jipipe.extensions.filesystem.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class OpenPathDataOperation implements JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        UIUtils.openFileInNative(((PathData) data).toPath());
    }

    @Override
    public String getId() {
        return "jipipe:open-path";
    }

    @Override
    public String getName() {
        return "Open";
    }

    @Override
    public String getDescription() {
        return "Opens the path as if opened from the file browser";
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/folder-open.png");
    }
}
