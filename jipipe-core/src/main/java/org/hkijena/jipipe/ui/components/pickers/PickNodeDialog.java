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

package org.hkijena.jipipe.ui.components.pickers;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.components.renderers.JIPipeAlgorithmListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI that adds slots to an algorithm
 */
public class PickNodeDialog extends JDialog {
    private Set<JIPipeGraphNode> nodes;
    private SearchTextField searchField;
    private JList<JIPipeGraphNode> nodeJList;
    private JIPipeGraphNode selectedNode;
    private JButton confirmButton;
    private boolean canceled = true;

    /**
     * @param parent      parent window
     * @param nodes       the available algorithms
     * @param preSelected selected node
     */
    public PickNodeDialog(Window parent, Set<JIPipeGraphNode> nodes, JIPipeGraphNode preSelected) {
        super(parent);
        this.nodes = nodes;
        initialize();
        reloadNodeList();
        if (preSelected == null) {
            preSelected = nodeJList.getSelectedValue();
        }
        setSelectedNode(preSelected);
        nodeJList.setSelectedValue(preSelected, true);
    }

    /**
     * Shows a dialog for selecting an algorithm
     *
     * @param parent      parent component
     * @param algorithms  available algorithms
     * @param preSelected optionally pre-selected node
     * @param title       the dialog title
     * @return the selected  algorithm or null of none was selected
     */
    public static JIPipeGraphNode showDialog(Component parent, Set<JIPipeGraphNode> algorithms, JIPipeGraphNode preSelected, String title) {
        PickNodeDialog dialog = new PickNodeDialog(SwingUtilities.getWindowAncestor(parent), algorithms, preSelected);
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 500));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
        if (!dialog.canceled)
            return dialog.getSelectedNode();
        else
            return null;
    }

    private void initialize() {
        setContentPane(new JPanel(new BorderLayout(8, 8)));
        initializeToolBar();

        nodeJList = new JList<>();
        nodeJList.setCellRenderer(new JIPipeAlgorithmListCellRenderer());
        nodeJList.addListSelectionListener(e -> {
            if (nodeJList.getSelectedValue() != null) {
                setSelectedNode(nodeJList.getSelectedValue());
            }
        });
        JScrollPane scrollPane = new JScrollPane(nodeJList);
        add(scrollPane, BorderLayout.CENTER);
        initializeButtonPanel();
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            canceled = true;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        confirmButton = new JButton("Pick", UIUtils.getIconFromResources("actions/color-select.png"));
        confirmButton.addActionListener(e -> pickAlgorithm());
        confirmButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                pickAlgorithm();
            }
        });
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void pickAlgorithm() {
        canceled = false;
        setVisible(false);
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadNodeList());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (selectedNode != null && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmButton.requestFocusInWindow();
                }
            }
        });
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);
    }

    private List<JIPipeGraphNode> getFilteredAndSortedInfos() {
        Predicate<JIPipeGraphNode> filterFunction = info -> searchField.test(info.getName());
        return nodes.stream().filter(filterFunction).sorted(Comparator.comparing(JIPipeGraphNode::getName)).collect(Collectors.toList());
    }

    private void reloadNodeList() {
        setSelectedNode(null);
        List<JIPipeGraphNode> available = getFilteredAndSortedInfos();
        DefaultListModel<JIPipeGraphNode> listModel = new DefaultListModel<>();
        int selectedIndex = -1;
        int index = 0;
        for (JIPipeGraphNode type : available) {
            listModel.addElement(type);
            if (type == selectedNode)
                selectedIndex = index;
            ++index;
        }
        nodeJList.setModel(listModel);
        if (selectedIndex >= 0) {
            nodeJList.setSelectedIndex(selectedIndex);
        } else {
            if (!listModel.isEmpty()) {
                nodeJList.setSelectedIndex(0);
            }
        }
    }

    public JIPipeGraphNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(JIPipeGraphNode selectedNode) {
        this.selectedNode = selectedNode;
    }
}
