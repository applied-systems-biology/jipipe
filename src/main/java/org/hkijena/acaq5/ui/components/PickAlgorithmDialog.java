package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
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
    private Set<ACAQAlgorithm> algorithms;
    private JXTextField searchField;
    private JList<ACAQAlgorithm> algorithmList;
    private ACAQAlgorithm selectedAlgorithm;
    private JButton confirmButton;
    private boolean canceled = true;

    /**
     * @param parent     parent window
     * @param algorithms the available algorithms
     */
    public PickAlgorithmDialog(Window parent, Set<ACAQAlgorithm> algorithms) {
        super(parent);
        this.algorithms = algorithms;
        initialize();
        reloadTypeList();
    }

    private void initialize() {
        setContentPane(new JPanel(new BorderLayout(8, 8)));
        initializeToolBar();

        algorithmList = new JList<>();
        algorithmList.setCellRenderer(new ACAQAlgorithmListCellRenderer());
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

        searchField = new JXTextField("Search ...");
        searchField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                reloadTypeList();
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (selectedAlgorithm != null && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmButton.requestFocusInWindow();
                }
            }
        });
        toolBar.add(searchField);

        JButton clearSearchButton = new JButton(UIUtils.getIconFromResources("clear.png"));
        clearSearchButton.addActionListener(e -> searchField.setText(null));
        toolBar.add(clearSearchButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private List<ACAQAlgorithm> getFilteredAndSortedDeclarations() {
        String[] searchStrings = getSearchStrings();
        Predicate<ACAQAlgorithm> filterFunction = declaration -> {
            if (searchStrings != null && searchStrings.length > 0) {
                boolean matches = true;
                String name = declaration.getName();
                for (String searchString : searchStrings) {
                    if (!name.toLowerCase().contains(searchString.toLowerCase())) {
                        matches = false;
                        break;
                    }
                }
                return matches;
            } else {
                return true;
            }
        };

        return algorithms.stream().filter(filterFunction).sorted(Comparator.comparing(ACAQAlgorithm::getName)).collect(Collectors.toList());
    }

    private void reloadTypeList() {
        setSelectedAlgorithm(null);
        List<ACAQAlgorithm> available = getFilteredAndSortedDeclarations();
        DefaultListModel<ACAQAlgorithm> listModel = new DefaultListModel<>();
        for (ACAQAlgorithm type : available) {
            listModel.addElement(type);
        }
        algorithmList.setModel(listModel);
        if (!listModel.isEmpty()) {
            algorithmList.setSelectedIndex(0);
        }
    }

    private String[] getSearchStrings() {
        String[] searchStrings = null;
        if (searchField.getText() != null) {
            String str = searchField.getText().trim();
            if (!str.isEmpty()) {
                searchStrings = str.split(" ");
            }
        }
        return searchStrings;
    }

    public ACAQAlgorithm getSelectedAlgorithm() {
        return selectedAlgorithm;
    }

    public void setSelectedAlgorithm(ACAQAlgorithm selectedAlgorithm) {
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
    public static ACAQAlgorithm showDialog(Component parent, Set<ACAQAlgorithm> algorithms, String title) {
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
