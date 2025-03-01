package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFancyTextField;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JIPipeDesktopFileChooserNext extends JPanel {

    private static final int TOOLBAR_BUTTON_SIZE = 42;
    private final JIPipeDesktopFormPanel leftPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JXTable entriesTable = new JXTable();
    private final JToolBar breadcrumbToolBar = new JToolBar();
    private final JIPipeDesktopFancyTextField breadcrumbPathEditor;
    private final JButton breadcrumbPathEditorConfirmButton;
    private Path currentDirectory;
    private final FileChooserHistory history = new FileChooserHistory();
    boolean breadcrumbEditMode = false;
    private final JCheckBoxMenuItem showHiddenToggle = new JCheckBoxMenuItem("Show hidden items");

    public JIPipeDesktopFileChooserNext() {
        this.breadcrumbPathEditor = new JIPipeDesktopFancyTextField(new JLabel(UIUtils.getIconFromResources("places/inode-directory.png")),
                "Enter the directory here", false);
        this.breadcrumbPathEditorConfirmButton = UIUtils.createButton("",
                UIUtils.getIconFromResources("actions/check.png"), this::confirmBreadcrumbPathEditor);
        UIUtils.makeButtonFlatWithSize(breadcrumbPathEditorConfirmButton, TOOLBAR_BUTTON_SIZE);

        initialize();
        setCurrentDirectory(Paths.get(""));
    }

    private void confirmBreadcrumbPathEditor() {
        try {
            Path dir = Paths.get(breadcrumbPathEditor.getText().trim());
            if (Files.isDirectory(dir)) {
                this.breadcrumbEditMode = false;
                setCurrentDirectory(dir);
            }
        } catch (Throwable ignored) {
        }
    }

    public void clearHistory() {
        history.clear();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel tablePanel = new JPanel(new BorderLayout());

        tablePanel.add(entriesTable.getTableHeader(), BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(entriesTable), BorderLayout.CENTER);
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        entriesTable.setModel(new JIPipeDesktopFileChooserNextTableModel(null, false, false));

        entriesTable.setShowGrid(false);
        entriesTable.setHighlighters(
                HighlighterFactory.createSimpleStriping(UIManager.getColor("Panel.background"))
        );

        // Configure renderers
        entriesTable.setRowHeight(34);
        entriesTable.setDefaultRenderer(Path.class, new JIPipeDesktopFileChooserNextPathTableCellRenderer());
        entriesTable.setDefaultRenderer(Long.class, new JIPipeDesktopFileChooserNextSizeTableCellRenderer());
        entriesTable.setDefaultRenderer(LocalDateTime.class, new JIPipeDesktopFileChooserNextDateTableCellRenderer());

        // Configure sorting
        entriesTable.setSortable(true);

        // Configure actions
        entriesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateSelected();
                }
            }
        });
        entriesTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigateSelected();
                }
            }
        });

        JToolBar topToolbar = new JToolBar();
        initializeToolbar(topToolbar);
        topToolbar.setFloatable(false);
        breadcrumbToolBar.setFloatable(false);
        breadcrumbToolBar.setMinimumSize(new Dimension(100, 42));
        breadcrumbToolBar.setPreferredSize(new Dimension(100, 42));
        breadcrumbToolBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(!breadcrumbEditMode) {
                    breadcrumbEditMode = true;
                    updateBreadcrumb();
                }
            }
        });
        breadcrumbPathEditor.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmBreadcrumbPathEditor();
                }
            }
        });

        mainPanel.add(UIUtils.borderNorthSouth(topToolbar, breadcrumbToolBar), BorderLayout.NORTH);

        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT,
                leftPanel,
                mainPanel,
                new JIPipeDesktopSplitPane.DynamicSidebarRatio(200, true));
        add(splitPane, BorderLayout.CENTER);

        leftPanel.addGroupHeader("Test", UIUtils.getIconFromResources("actions/add.png"));
    }

    private void navigateSelected() {
        int selectedRow = entriesTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = entriesTable.convertRowIndexToModel(selectedRow);
            Path path = (Path) entriesTable.getModel().getValueAt(modelRow, 0);

            try {
                if (Files.isDirectory(path)) {
                    setCurrentDirectory(path);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Unable to navigate to " + path.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void initializeToolbar(JToolBar toolbar) {
        toolbar.add(UIUtils.makeButtonFlatWithSize(UIUtils.createButton("", UIUtils.getIconFromResources("actions/back.png"), this::goBack), TOOLBAR_BUTTON_SIZE));
        toolbar.add(UIUtils.makeButtonFlatWithSize(UIUtils.createButton("", UIUtils.getIconFromResources("actions/next.png"), this::goNext), TOOLBAR_BUTTON_SIZE));
        toolbar.add(UIUtils.makeButtonFlatWithSize(UIUtils.createButton("", UIUtils.getIconFromResources("actions/up.png"), this::goUp), TOOLBAR_BUTTON_SIZE));
        toolbar.add(UIUtils.makeButtonFlatWithSize(UIUtils.createButton("", UIUtils.getIconFromResources("actions/view-refresh.png"), this::refresh), TOOLBAR_BUTTON_SIZE));
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(UIUtils.makeButtonFlat(UIUtils.createButton("New directory", UIUtils.getIconFromResources("actions/archive-insert-directory.png"), this::createDirectory)));

        JButton menuButton = UIUtils.makeButtonFlatWithSize(new JButton(UIUtils.getIcon16FromResources("actions/hamburger-menu.png")), TOOLBAR_BUTTON_SIZE);
        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(menuButton);
        popupMenu.add(showHiddenToggle);
        toolbar.add(menuButton);

        showHiddenToggle.addActionListener(e -> { refresh(); });
    }

    private void createDirectory() {
        String name = JOptionPane.showInputDialog(this, "Please enter the name of the newly created directory");
        if(!StringUtils.isNullOrEmpty(name)) {
            try {
                Files.createDirectory(currentDirectory.resolve(name));
                refresh();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Unable to create new directory", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refresh() {
        JIPipeDesktopFileChooserNextTableModel model = new JIPipeDesktopFileChooserNextTableModel(currentDirectory, true, showHiddenToggle.isSelected());

        // Grab the old sort order
        int targetColumn = -1;
        SortOrder targetSortOrder = SortOrder.UNSORTED;
        for (int i = 0; i < model.getColumnCount(); i++) {
            SortOrder sortOrder = entriesTable.getSortOrder(i);
            if (sortOrder != SortOrder.UNSORTED) {
                targetColumn = i;
                targetSortOrder = sortOrder;
            }
        }

        entriesTable.setModel(model);

        if (targetSortOrder != SortOrder.UNSORTED) {
            entriesTable.setSortOrder(targetColumn, targetSortOrder);
        } else {
            // Default sort order
            entriesTable.setSortOrder(0, SortOrder.ASCENDING);
        }

        updateBreadcrumb();

    }

    private void goUp() {
        if (currentDirectory != null && currentDirectory.getParent() != null) {
            setCurrentDirectory(currentDirectory.getParent());
        }
    }

    private void goNext() {
        if (history.canGoForward()) {
            Path next = history.goForward();
            if (next != null) {
                this.currentDirectory = next.toAbsolutePath();
                refresh();
            }
        }
    }

    private void goBack() {
        if (history.canGoBack()) {
            Path next = history.goBack();
            if (next != null) {
                this.currentDirectory = next.toAbsolutePath();
                refresh();
            }
        }
    }

    private void updateBreadcrumb() {
        breadcrumbToolBar.removeAll();
        if (breadcrumbEditMode) {
            breadcrumbPathEditor.setText(currentDirectory.toAbsolutePath().toString());
            breadcrumbToolBar.add(breadcrumbPathEditor);
            breadcrumbToolBar.add(breadcrumbPathEditorConfirmButton);
            breadcrumbToolBar.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else {
            breadcrumbToolBar.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

            int fullSegmentWidth = 0;
            JPopupMenu overhangMenu = null;

            Path current = currentDirectory;
            do {
                JButton navigateButton = new JButton(StringUtils.orElse(current.getFileName(), "/"));
                navigateButton.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                navigateButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                JButton listChildrenButton = new JButton(UIUtils.getIcon12FromResources("actions/go-right.png"));
                listChildrenButton.setBorder(BorderFactory.createEmptyBorder(6, 3, 6, 3));
                listChildrenButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                int segmentWidth = navigateButton.getFontMetrics(navigateButton.getFont()).stringWidth(navigateButton.getText()) + 6 + 12 + 6;
                fullSegmentWidth += segmentWidth;

                if((fullSegmentWidth + 64) >= breadcrumbToolBar.getWidth() && overhangMenu == null) {
                    // Create a navigation menu that will be used instead
                    breadcrumbToolBar.add(listChildrenButton, 0);
                    overhangMenu = new JPopupMenu();
                    UIUtils.addPopupMenuToButton(listChildrenButton, overhangMenu);
                }

                Path finalCurrent = current;

                if(overhangMenu == null) {
                    breadcrumbToolBar.add(listChildrenButton, 0);
                    breadcrumbToolBar.add(navigateButton, 0);

                    // Wire buttons

                    navigateButton.addActionListener(e -> {
                        setCurrentDirectory(finalCurrent);
                    });

                    JPopupMenu popupMenu = new JPopupMenu();
                    UIUtils.addReloadablePopupMenuToButton(listChildrenButton, popupMenu, () -> {
                        popupMenu.removeAll();
                        try {
                            PathUtils.listSubDirectories(finalCurrent).stream().sorted(Comparator.comparing(Path::toString)).forEach(path -> {
                                try {
                                    if (!showHiddenToggle.isSelected()) {
                                        if (path.getFileName().toString().startsWith(".") || Files.isHidden(path)) {
                                            return;
                                        }
                                    }
                                    popupMenu.add(UIUtils.createMenuItem(path.getFileName().toString(), path.toString(), null, () -> {
                                        setCurrentDirectory(path);
                                    }));
                                }
                                catch (Exception ex) {}
                            });
                        }
                        catch (Exception ex) {}
                    });
                }
                else {
                    overhangMenu.add(UIUtils.createMenuItem(StringUtils.orElse(current.getFileName(), "/"), current.toString(), null, () -> {
                        setCurrentDirectory(finalCurrent);
                    }), 0);
                }

                // Find parent
                current = current.getParent();
            }
            while (current != null);

            // Add final spacer
            breadcrumbToolBar.add(Box.createHorizontalStrut(8), 0);
        }
        breadcrumbToolBar.revalidate();
        breadcrumbToolBar.repaint(50);
    }

    public static List<Path> showDialog(JComponent parent, String title) {
        JDialog dialog = new JDialog(parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setIconImage(UIUtils.getJIPipeIcon128());

        JPanel mainPanel = new JPanel(new BorderLayout());

        JIPipeDesktopFileChooserNext panel = new JIPipeDesktopFileChooserNext();
        mainPanel.add(panel, BorderLayout.CENTER);

        dialog.setContentPane(mainPanel);
        dialog.setLocationRelativeTo(parent);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setVisible(true);

        return panel.getSelectedPaths();
    }

    private List<Path> getSelectedPaths() {
        return null;
    }

    public static void main(String[] args) {
        JIPipeDesktopUITheme.ModernLight.install();
        JIPipeDesktopFileChooserNext.showDialog(null, "Test dialog");
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(Path currentDirectory) {
        this.currentDirectory = currentDirectory.toAbsolutePath();
        this.history.insert(currentDirectory);
        refresh();
    }
}
