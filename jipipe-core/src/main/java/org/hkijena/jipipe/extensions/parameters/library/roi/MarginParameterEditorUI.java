/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.library.roi;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Editor for {@link Margin}
 */
public class MarginParameterEditorUI extends JIPipeParameterEditorUI {

    private boolean skipNextReload = false;
    private Map<Anchor, JToggleButton> anchorSelectionMap = new HashMap<>();
    private ParameterPanel parameterPanel;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public MarginParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        toolBar.add(new JLabel(getParameterAccess().getName()));

        JPanel content = new JPanel(new BorderLayout());
        initializeAnchorSelection(content);

        parameterPanel = new ParameterPanel(getWorkbench(), null, null, ParameterPanel.NO_EMPTY_GROUP_HEADERS);
        content.add(parameterPanel, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    private void initializeAnchorSelection(JPanel content) {
        JPanel anchorPanel = new JPanel(new GridLayout(3, 3));
        ButtonGroup anchorGroup = new ButtonGroup();

        // Top
        JToggleButton topLeft = new JToggleButton(UIUtils.getIconFromResources("actions/anchor-top-left.png"));
        UIUtils.makeFlat(topLeft);
        topLeft.setPreferredSize(new Dimension(25, 25));
        anchorGroup.add(topLeft);
        anchorPanel.add(topLeft);
        anchorSelectionMap.put(Anchor.TopLeft, topLeft);

        JToggleButton topCenter = new JToggleButton(UIUtils.getIconFromResources("actions/anchor-top-center.png"));
        UIUtils.makeFlat(topCenter);
        anchorGroup.add(topCenter);
        anchorPanel.add(topCenter);
        anchorSelectionMap.put(Anchor.TopCenter, topCenter);

        JToggleButton topRight = new JToggleButton(UIUtils.getIconFromResources("actions/anchor-top-right.png"));
        UIUtils.makeFlat(topRight);
        anchorGroup.add(topRight);
        anchorPanel.add(topRight);
        anchorSelectionMap.put(Anchor.TopRight, topRight);

        // Center
        JToggleButton centerLeft = new JToggleButton(UIUtils.getIconFromResources("actions/anchor-center-left.png"));
        UIUtils.makeFlat(centerLeft);
        anchorGroup.add(centerLeft);
        anchorPanel.add(centerLeft);
        anchorSelectionMap.put(Anchor.CenterLeft, centerLeft);

        JToggleButton centerCenter = new JToggleButton(UIUtils.getIconFromResources("actions/anchor-center-center.png"));
        UIUtils.makeFlat(centerCenter);
        anchorGroup.add(centerCenter);
        anchorPanel.add(centerCenter);
        anchorSelectionMap.put(Anchor.CenterCenter, centerCenter);

        JToggleButton centerRight = new JToggleButton(UIUtils.getIconFromResources("actions/anchor-center-right.png"));
        UIUtils.makeFlat(centerRight);
        anchorGroup.add(centerRight);
        anchorPanel.add(centerRight);
        anchorSelectionMap.put(Anchor.CenterRight, centerRight);

        // Bottom
        JToggleButton bottomLeft = new JToggleButton(UIUtils.getIconFromResources("actions/anchor-bottom-left.png"));
        UIUtils.makeFlat(bottomLeft);
        anchorGroup.add(bottomLeft);
        anchorPanel.add(bottomLeft);
        anchorSelectionMap.put(Anchor.BottomLeft, bottomLeft);

        JToggleButton bottomCenter = new JToggleButton(UIUtils.getIconFromResources("actions/anchor-bottom-center.png"));
        UIUtils.makeFlat(bottomCenter);
        anchorGroup.add(bottomCenter);
        anchorPanel.add(bottomCenter);
        anchorSelectionMap.put(Anchor.BottomCenter, bottomCenter);

        JToggleButton bottomRight = new JToggleButton(UIUtils.getIconFromResources("actions/anchor-bottom-right.png"));
        UIUtils.makeFlat(bottomRight);
        anchorGroup.add(bottomRight);
        anchorPanel.add(bottomRight);
        anchorSelectionMap.put(Anchor.BottomRight, bottomRight);

        content.add(anchorPanel, BorderLayout.NORTH);

        for (Map.Entry<Anchor, JToggleButton> entry : anchorSelectionMap.entrySet()) {
            entry.getValue().addActionListener(e -> {
                if (entry.getValue().isSelected()) {
                    Margin roi = getParameter(Margin.class);
                    roi.setAnchor(entry.getKey());
                    setParameter(roi, true);
                }
            });
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        Margin roi = getParameter(Margin.class);
        anchorSelectionMap.get(roi.getAnchor()).setSelected(true);

        // Update the parameter panel
        JIPipeParameterTree traversedParameterCollection = new JIPipeParameterTree(roi);
        Set<String> relevantParameterKeys = roi.getRelevantParameterKeys();
        for (String s : ImmutableList.copyOf(traversedParameterCollection.getParameters().keySet())) {
            if (!relevantParameterKeys.contains(s)) {
                traversedParameterCollection.getParameters().remove(s);
            }
        }
        parameterPanel.setDisplayedParameters(traversedParameterCollection);

    }
}
