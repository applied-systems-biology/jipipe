package org.hkijena.acaq5.extensions.parameters.editors;

import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQTraversedParameterCollection;
import org.hkijena.acaq5.extensions.parameters.roi.RectangleROIDefinitionParameter;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Editor for {@link org.hkijena.acaq5.extensions.parameters.roi.RectangleROIDefinitionParameter}
 */
public class RectangleROIDefinitionParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private Map<RectangleROIDefinitionParameter.Anchor, JToggleButton> anchorSelectionMap = new HashMap<>();
    private ParameterPanel parameterPanel;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public RectangleROIDefinitionParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
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

        parameterPanel = new ParameterPanel(getContext(), null, null, ParameterPanel.NO_EMPTY_GROUP_HEADERS);
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
        anchorSelectionMap.put(RectangleROIDefinitionParameter.Anchor.TopLeft, topLeft);

        JToggleButton topCenter = new JToggleButton(UIUtils.getIconFromResources("anchor-top-center.png"));
        UIUtils.makeFlat(topCenter);
        anchorGroup.add(topCenter);
        anchorPanel.add(topCenter);
        anchorSelectionMap.put(RectangleROIDefinitionParameter.Anchor.TopCenter, topCenter);

        JToggleButton topRight = new JToggleButton(UIUtils.getIconFromResources("anchor-top-right.png"));
        UIUtils.makeFlat(topRight);
        anchorGroup.add(topRight);
        anchorPanel.add(topRight);
        anchorSelectionMap.put(RectangleROIDefinitionParameter.Anchor.TopRight, topRight);

        // Center
        JToggleButton centerLeft = new JToggleButton(UIUtils.getIconFromResources("anchor-center-left.png"));
        UIUtils.makeFlat(centerLeft);
        anchorGroup.add(centerLeft);
        anchorPanel.add(centerLeft);
        anchorSelectionMap.put(RectangleROIDefinitionParameter.Anchor.CenterLeft, centerLeft);

        JToggleButton centerCenter = new JToggleButton(UIUtils.getIconFromResources("move.png"));
        UIUtils.makeFlat(centerCenter);
        anchorGroup.add(centerCenter);
        anchorPanel.add(centerCenter);
        anchorSelectionMap.put(RectangleROIDefinitionParameter.Anchor.CenterCenter, centerCenter);

        JToggleButton centerRight = new JToggleButton(UIUtils.getIconFromResources("anchor-center-right.png"));
        UIUtils.makeFlat(centerRight);
        anchorGroup.add(centerRight);
        anchorPanel.add(centerRight);
        anchorSelectionMap.put(RectangleROIDefinitionParameter.Anchor.CenterRight, centerRight);

        // Bottom
        JToggleButton bottomLeft = new JToggleButton(UIUtils.getIconFromResources("anchor-bottom-left.png"));
        UIUtils.makeFlat(bottomLeft);
        anchorGroup.add(bottomLeft);
        anchorPanel.add(bottomLeft);
        anchorSelectionMap.put(RectangleROIDefinitionParameter.Anchor.BottomLeft, bottomLeft);

        JToggleButton bottomCenter = new JToggleButton(UIUtils.getIconFromResources("anchor-bottom-center.png"));
        UIUtils.makeFlat(bottomCenter);
        anchorGroup.add(bottomCenter);
        anchorPanel.add(bottomCenter);
        anchorSelectionMap.put(RectangleROIDefinitionParameter.Anchor.BottomCenter, bottomCenter);

        JToggleButton bottomRight = new JToggleButton(UIUtils.getIconFromResources("anchor-bottom-right.png"));
        UIUtils.makeFlat(bottomRight);
        anchorGroup.add(bottomRight);
        anchorPanel.add(bottomRight);
        anchorSelectionMap.put(RectangleROIDefinitionParameter.Anchor.BottomRight, bottomRight);

        content.add(anchorPanel, BorderLayout.NORTH);

        for (Map.Entry<RectangleROIDefinitionParameter.Anchor, JToggleButton> entry : anchorSelectionMap.entrySet()) {
            entry.getValue().addActionListener(e -> {
                if (entry.getValue().isSelected()) {
                    RectangleROIDefinitionParameter roi = getParameterAccess().get(RectangleROIDefinitionParameter.class);
                    if (roi == null) {
                        roi = new RectangleROIDefinitionParameter();
                        getParameterAccess().set(roi);
                        return;
                    }
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
        RectangleROIDefinitionParameter roi = getParameterAccess().get(RectangleROIDefinitionParameter.class);
        if (roi == null) {
            roi = new RectangleROIDefinitionParameter();
            getParameterAccess().set(roi);
            return;
        }
        anchorSelectionMap.get(roi.getAnchor()).setSelected(true);

        // Update the parameter panel
        ACAQTraversedParameterCollection traversedParameterCollection = new ACAQTraversedParameterCollection(roi);
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
