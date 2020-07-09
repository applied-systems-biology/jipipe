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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
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
public class PickAlgorithmDialog extends JDialog {
    private Set<JIPipeGraphNode> algorithms;
    private SearchTextField searchField;
    private JList<JIPipeGraphNode> algorithmList;
    private JIPipeGraphNode selectedAlgorithm;
    private JButton confirmButton;
    private boolean canceled = true;

    /**
     * @param parent     parent window
     * @param algorithms the available algorithms
     */
    public PickAlgorithmDialog(Window parent, Set<JIPipeGraphNode> algorithms) {
        super(parent);
        this.algorithms = algorithms;
        initialize();
        reloadTypeList();
    }

    private void initialize() {
        setContentPane(new JPanel(new BorderLayout(8, 8)));
        initializeToolBar();

        algorithmList = new JList<>();
        algorithmList.setCellRenderer(new JIPipeAlgorithmListCellRenderer());
        algorithmList.addListSelectionListener(e -> {
            if (algorithmList.getSelectedValue() != null) {
                setSelectedAlgorithm(algorithmList.getSelectedValue());
            }
        });
        JScrollPane scrollPane = new JScrollPane(algorithmList);
        add(scrollPane, BorderLayout.CENTER);
        initializeButtonPanel();
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            canceled = true;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        confirmButton = new JButton("Pick", UIUtils.getIconFromResources("pick.png"));
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
        searchField.addActionListener(e -> reloadTypeList());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (selectedAlgorithm != null && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmButton.requestFocusInWindow();
                }
            }
        });
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);
    }

    private List<JIPipeGraphNode> getFilteredAndSortedDeclarations() {
        Predicate<JIPipeGraphNode> filterFunction = declaration -> searchField.test(declaration.getName());
        return algorithms.stream().filter(filterFunction).sorted(Comparator.comparing(JIPipeGraphNode::getName)).collect(Collectors.toList());
    }

    private void reloadTypeList() {
        setSelectedAlgorithm(null);
        List<JIPipeGraphNode> available = getFilteredAndSortedDeclarations();
        DefaultListModel<JIPipeGraphNode> listModel = new DefaultListModel<>();
        for (JIPipeGraphNode type : available) {
            listModel.addElement(type);
        }
        algorithmList.setModel(listModel);
        if (!listModel.isEmpty()) {
            algorithmList.setSelectedIndex(0);
        }
    }

    public JIPipeGraphNode getSelectedAlgorithm() {
        return selectedAlgorithm;
    }

    public void setSelectedAlgorithm(JIPipeGraphNode selectedAlgorithm) {
        this.selectedAlgorithm = selectedAlgorithm;
    }

    /**
     * Shows a dialog for selecting an algorithm
     *
     * @param parent     parent component
     * @param algorithms available algorithms
     * @param title      the dialog title
     * @return the selected  algorithm or null of none was selected
     */
    public static JIPipeGraphNode showDialog(Component parent, Set<JIPipeGraphNode> algorithms, String title) {
        PickAlgorithmDialog dialog = new PickAlgorithmDialog(SwingUtilities.getWindowAncestor(parent), algorithms);
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 500));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
        if (!dialog.canceled)
            return dialog.getSelectedAlgorithm();
        else
            return null;
    }
}
