package org.hkijena.acaq5.extensions.parameters.roi;

import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Editor for {@link Margin}
 */
public class MarginParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private Map<Margin.Anchor, JToggleButton> anchorSelectionMap = new HashMap<>();
    private ParameterPanel parameterPanel;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public MarginParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
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
        JToggleButton topLeft = new JToggleButton(UIUtils.getIconFromResources("anchor-top-left.png"));
        UIUtils.makeFlat(topLeft);
        topLeft.setPreferredSize(new Dimension(25, 25));
        anchorGroup.add(topLeft);
        anchorPanel.add(topLeft);
        anchorSelectionMap.put(Margin.Anchor.TopLeft, topLeft);

        JToggleButton topCenter = new JToggleButton(UIUtils.getIconFromResources("anchor-top-center.png"));
        UIUtils.makeFlat(topCenter);
        anchorGroup.add(topCenter);
        anchorPanel.add(topCenter);
        anchorSelectionMap.put(Margin.Anchor.TopCenter, topCenter);

        JToggleButton topRight = new JToggleButton(UIUtils.getIconFromResources("anchor-top-right.png"));
        UIUtils.makeFlat(topRight);
        anchorGroup.add(topRight);
        anchorPanel.add(topRight);
        anchorSelectionMap.put(Margin.Anchor.TopRight, topRight);

        // Center
        JToggleButton centerLeft = new JToggleButton(UIUtils.getIconFromResources("anchor-center-left.png"));
        UIUtils.makeFlat(centerLeft);
        anchorGroup.add(centerLeft);
        anchorPanel.add(centerLeft);
        anchorSelectionMap.put(Margin.Anchor.CenterLeft, centerLeft);

        JToggleButton centerCenter = new JToggleButton(UIUtils.getIconFromResources("move.png"));
        UIUtils.makeFlat(centerCenter);
        anchorGroup.add(centerCenter);
        anchorPanel.add(centerCenter);
        anchorSelectionMap.put(Margin.Anchor.CenterCenter, centerCenter);

        JToggleButton centerRight = new JToggleButton(UIUtils.getIconFromResources("anchor-center-right.png"));
        UIUtils.makeFlat(centerRight);
        anchorGroup.add(centerRight);
        anchorPanel.add(centerRight);
        anchorSelectionMap.put(Margin.Anchor.CenterRight, centerRight);

        // Bottom
        JToggleButton bottomLeft = new JToggleButton(UIUtils.getIconFromResources("anchor-bottom-left.png"));
        UIUtils.makeFlat(bottomLeft);
        anchorGroup.add(bottomLeft);
        anchorPanel.add(bottomLeft);
        anchorSelectionMap.put(Margin.Anchor.BottomLeft, bottomLeft);

        JToggleButton bottomCenter = new JToggleButton(UIUtils.getIconFromResources("anchor-bottom-center.png"));
        UIUtils.makeFlat(bottomCenter);
        anchorGroup.add(bottomCenter);
        anchorPanel.add(bottomCenter);
        anchorSelectionMap.put(Margin.Anchor.BottomCenter, bottomCenter);

        JToggleButton bottomRight = new JToggleButton(UIUtils.getIconFromResources("anchor-bottom-right.png"));
        UIUtils.makeFlat(bottomRight);
        anchorGroup.add(bottomRight);
        anchorPanel.add(bottomRight);
        anchorSelectionMap.put(Margin.Anchor.BottomRight, bottomRight);

        content.add(anchorPanel, BorderLayout.NORTH);

        for (Map.Entry<Margin.Anchor, JToggleButton> entry : anchorSelectionMap.entrySet()) {
            entry.getValue().addActionListener(e -> {
                if (entry.getValue().isSelected()) {
                    Margin roi = getParameter(Margin.class);
                    roi.setAnchor(entry.getKey());
                    getParameterAccess().set(roi);
                    reload();
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
        isReloading = true;
        Margin roi = getParameter(Margin.class);
        anchorSelectionMap.get(roi.getAnchor()).setSelected(true);

        // Update the parameter panel
        ACAQParameterTree traversedParameterCollection = new ACAQParameterTree(roi);
        Set<String> relevantParameterKeys = roi.getRelevantParameterKeys();
        for (String s : ImmutableList.copyOf(traversedParameterCollection.getParameters().keySet())) {
            if (!relevantParameterKeys.contains(s)) {
                traversedParameterCollection.getParameters().remove(s);
            }
        }
        parameterPanel.setDisplayedParameters(traversedParameterCollection);

        isReloading = false;
    }
}
