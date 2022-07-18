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
 *
 */

package org.hkijena.jipipe.ui.cache;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.components.layouts.ModifiedFlowLayout;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JIPipeAlgorithmCacheBrowserOutputSelectorUI extends JPanel {

    public static final String SELECT_ALL_OUTPUTS = "";

    private final EventBus eventBus = new EventBus();
    private final JIPipeGraphNode graphNode;

    private final Map<String, JToggleButton> buttonMap = new HashMap<>();
    private final List<String> outputOrder = new ArrayList<>();
    private String selectedOutput;

    public JIPipeAlgorithmCacheBrowserOutputSelectorUI(JIPipeGraphNode graphNode) {
        this.graphNode = graphNode;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JButton previousButton = new JButton(UIUtils.getIconFromResources("actions/draw-triangle1.png"));
        previousButton.setBorder(null);
        previousButton.addActionListener(e -> showPrevious());
        add(previousButton, BorderLayout.WEST);

        JButton nextButton = new JButton(UIUtils.getIconFromResources("actions/draw-triangle2.png"));
        nextButton.setBorder(null);
        nextButton.addActionListener(e -> showNext());
        add(nextButton, BorderLayout.EAST);

        JPanel wrapperPanel = new JPanel(new ModifiedFlowLayout(FlowLayout.CENTER));
        {
            JToggleButton outputButton = new JToggleButton("All outputs", UIUtils.getIconFromResources("actions/stock_select-all.png"));
            outputButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            wrapperPanel.add(outputButton);
            buttonMap.put("", outputButton);
            outputOrder.add("");
            outputButton.addActionListener(e -> selectOutput(SELECT_ALL_OUTPUTS));
        }
        for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
            JToggleButton outputButton = new JToggleButton(outputSlot.getName(), JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()));
            outputButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            wrapperPanel.add(outputButton);
            buttonMap.put(outputSlot.getName(), outputButton);
            outputOrder.add(outputSlot.getName());
            outputButton.addActionListener(e -> selectOutput(outputSlot.getName()));
        }

        add(wrapperPanel, BorderLayout.CENTER);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Selects an output
     *
     * @param name the name. if left empty, all outputs will be selected.
     */
    public void selectOutput(String name) {
        if (!buttonMap.containsKey(name)) {
            name = "";
        }
        this.selectedOutput = name;
        for (Map.Entry<String, JToggleButton> entry : buttonMap.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(name));
        }
        eventBus.post(new OutputSelectedEvent(this, name));
    }

    public String getSelectedOutput() {
        return selectedOutput;
    }

    private void showPrevious() {
        int i = outputOrder.indexOf(selectedOutput);
        if (i - 1 < 0) {
            i = outputOrder.size() - 1;
        } else {
            --i;
        }
        selectOutput(outputOrder.get(i));
    }

    private void showNext() {
        int i = outputOrder.indexOf(selectedOutput);
        if (i < 0)
            i = 0;
        selectOutput(outputOrder.get((i + 1) % outputOrder.size()));
    }

    public static class OutputSelectedEvent {
        private final JIPipeAlgorithmCacheBrowserOutputSelectorUI selectorUI;
        private final String name;

        public OutputSelectedEvent(JIPipeAlgorithmCacheBrowserOutputSelectorUI selectorUI, String name) {
            this.selectorUI = selectorUI;
            this.name = name;
        }

        public JIPipeAlgorithmCacheBrowserOutputSelectorUI getSelectorUI() {
            return selectorUI;
        }

        public String getName() {
            return name;
        }
    }
}
