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

package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface JIPipeNodeDatabaseEntry {

    String getId();

    WeightedTokens getTokens();

    boolean exists();

    JIPipeNodeDatabasePipelineVisibility getVisibility();

    String getName();

    HTMLText getDescription();

    Icon getIcon();

    List<String> getLocationInfos();

    Set<String> getCategoryIds();

    Map<String, JIPipeDataSlotInfo> getInputSlots();

    Map<String, JIPipeDataSlotInfo> getOutputSlots();

    Color getFillColor();

    Color getBorderColor();

    Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI);

    boolean canAddInputSlots();

    boolean canAddOutputSlots();

    boolean isDeprecated();
}
