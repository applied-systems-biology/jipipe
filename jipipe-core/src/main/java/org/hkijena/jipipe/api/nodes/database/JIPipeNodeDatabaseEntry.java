package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public interface JIPipeNodeDatabaseEntry {

    String getId();
    List<String> getTokens();
    boolean exists();
    JIPipeNodeDatabaseRole getRole();
    String getName();
    HTMLText getDescription();
    Icon getIcon();
    String getCategory();
    Map<String, JIPipeDataSlotInfo> getInputSlots();
    Map<String, JIPipeDataSlotInfo> getOutputSlots();
}
