package org.hkijena.jipipe.desktop.app.cache;

import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopAlgorithmListCellRenderer;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * Can display the caches of multiple algorithms
 */
public class JIPipeDesktopMultiAlgorithmCacheBrowserUI extends JIPipeDesktopProjectWorkbenchPanel {

    private final JComboBox<JIPipeAlgorithm> algorithmComboBox = new JComboBox<>();
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private JIPipeDesktopAlgorithmCacheBrowserUI currentBrowser;
    private List<JIPipeAlgorithm> displayedAlgorithms;

    public JIPipeDesktopMultiAlgorithmCacheBrowserUI(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);

        initialize();
        refreshContentPanel();

        algorithmComboBox.addActionListener(e -> {
           refreshContentPanel();
        });
    }

    private void refreshContentPanel() {
        if (algorithmComboBox.getSelectedItem() == null) {
            contentPanel.removeAll();
            contentPanel.add(UIUtils.createInfoLabel("Nothing to display", "Currently no algorithm selected"), BorderLayout.CENTER);
            revalidate();
            repaint(50);
        } else {
            if (currentBrowser == null || currentBrowser.getGraphNode() != algorithmComboBox.getSelectedItem()) {
                contentPanel.removeAll();
                currentBrowser = new JIPipeDesktopAlgorithmCacheBrowserUI(getDesktopProjectWorkbench(), (JIPipeGraphNode) algorithmComboBox.getSelectedItem(), null);
                contentPanel.add(currentBrowser, BorderLayout.CENTER);
                revalidate();
                repaint(50);
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        algorithmComboBox.setRenderer(new JIPipeDesktopAlgorithmListCellRenderer());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(UIUtils.createButton("", UIUtils.getIconFromResources("actions/caret-left.png"), this::goToPrevious), BorderLayout.WEST);
        topPanel.add(UIUtils.createButton("", UIUtils.getIconFromResources("actions/caret-right.png"), this::goToNext), BorderLayout.EAST);
        topPanel.add(algorithmComboBox, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.getControlBorderColor()));

        add(topPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void goToNext() {
        int itemCount = algorithmComboBox.getItemCount();
        if (itemCount == 0) {
            // No items at all, nothing to do
            return;
        }

        int currentIndex = algorithmComboBox.getSelectedIndex();
        if (currentIndex == -1) {
            // Nothing is selected, so just select the first item
            algorithmComboBox.setSelectedIndex(0);
        } else {
            // Cycle to next item, wrap around if needed
            int nextIndex = (currentIndex + 1) % itemCount;
            algorithmComboBox.setSelectedIndex(nextIndex);
        }
    }

    private void goToPrevious() {
        int itemCount = algorithmComboBox.getItemCount();
        if (itemCount == 0) {
            // No items at all, nothing to do
            return;
        }

        int currentIndex = algorithmComboBox.getSelectedIndex();
        if (currentIndex == -1) {
            // Nothing is selected, so just select the first item
            algorithmComboBox.setSelectedIndex(0);
        } else {
            // Cycle to previous item, wrap around if needed
            int previousIndex = (currentIndex - 1 + itemCount) % itemCount;
            algorithmComboBox.setSelectedIndex(previousIndex);
        }
    }

    public void setDisplayedAlgorithms(List<JIPipeAlgorithm> displayedAlgorithms) {
        this.displayedAlgorithms = displayedAlgorithms;
        updateComboBox();
    }

    private void updateComboBox() {
        Object item = algorithmComboBox.getSelectedItem();
        boolean found = false;

        DefaultComboBoxModel<JIPipeAlgorithm> model = new DefaultComboBoxModel<>();
        for (JIPipeAlgorithm algorithm : displayedAlgorithms) {
            model.addElement(algorithm);

            if (item != null && algorithm == item) {
                found = true;
            }
        }

        algorithmComboBox.setModel(model);
        if(found) {
            algorithmComboBox.setSelectedItem(item);
        }
        else {
            if(model.getSize() > 0) {
                algorithmComboBox.setSelectedIndex(0);
            }
        }
    }

    public List<JIPipeAlgorithm> getDisplayedAlgorithms() {
        return Collections.unmodifiableList(displayedAlgorithms);
    }
}
