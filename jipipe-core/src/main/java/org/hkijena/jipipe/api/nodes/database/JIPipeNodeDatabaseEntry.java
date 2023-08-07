package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public interface JIPipeNodeDatabaseEntry {

    String getId();
    WeightedTokens getTokens();
    boolean exists();
    JIPipeNodeDatabaseRole getRole();
    String getName();
    HTMLText getDescription();
    String getDescriptionPlain();
    Icon getIcon();
    String getCategory();
    Map<String, JIPipeDataSlotInfo> getInputSlots();
    Map<String, JIPipeDataSlotInfo> getOutputSlots();
    Color getFillColor();
    Color getBorderColor();
    JIPipeGraphNodeUI addToGraph(JIPipeGraphCanvasUI canvasUI);
    boolean canAddInputSlots();
    boolean canAddOutputSlots();
}
