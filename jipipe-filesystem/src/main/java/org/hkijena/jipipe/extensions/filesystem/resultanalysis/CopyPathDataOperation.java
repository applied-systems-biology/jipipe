package org.hkijena.jipipe.extensions.filesystem.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class CopyPathDataOperation implements JIPipeDataDisplayOperation {

    @Override
    public String getId() {
        return "jipipe:copy-path";
    }

    @Override
    public String getName() {
        return "Copy path";
    }

    @Override
    public String getDescription() {
        return "Opens the path as if opened from the file browser";
    }

    @Override
    public int getOrder() {
        return 1500;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-copy.png");
    }

    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        String string = ((PathData) data).toPath().toString();
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
