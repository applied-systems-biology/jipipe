package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JIPipeDesktopFileChooserNext extends JPanel {

    private static final int TOOLBAR_BUTTON_SIZE = 42;
    private final JIPipeDesktopFormPanel leftPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JXTable entriesTable = new JXTable();
    private final JToolBar breadcrumb = new JToolBar();
    private Path currentDirectory;
    private final FileChooserHistory history = new FileChooserHistory();

    public JIPipeDesktopFileChooserNext() {
        initialize();
        setCurrentDirectory(Paths.get(""));
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

        entriesTable.setModel(new JIPipeDesktopFileChooserNextTableModel(null));

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
                if(e.getClickCount() == 2) {
                    navigateSelected();
                }
            }
        });
        entriesTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigateSelected();
                }
            }
        });

        JToolBar topToolbar = new JToolBar();
        initializeToolbar(topToolbar);
        topToolbar.setFloatable(false);
        breadcrumb.setFloatable(false);

        mainPanel.add(UIUtils.borderNorthSouth(topToolbar, breadcrumb), BorderLayout.NORTH);

        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT,
                leftPanel,
                mainPanel,
                new JIPipeDesktopSplitPane.DynamicSidebarRatio(200, true));
        add(splitPane, BorderLayout.CENTER);

        leftPanel.addGroupHeader("Test", UIUtils.getIconFromResources("actions/add.png"));
    }

    private void navigateSelected() {
        int selectedRow = entriesTable.getSelectedRow();
        if(selectedRow >= 0) {
            int modelRow = entriesTable.convertRowIndexToModel(selectedRow);
            Path path = (Path) entriesTable.getModel().getValueAt(modelRow, 0);

            try {
                if(Files.isDirectory(path)) {
                    setCurrentDirectory(path);
                }
            }
            catch (Exception e) {
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
    }

    private void createDirectory() {

    }

    private void refresh() {
        JIPipeDesktopFileChooserNextTableModel model = new JIPipeDesktopFileChooserNextTableModel(currentDirectory);

        // Grab the old sort order
        int targetColumn = -1;
        SortOrder targetSortOrder = SortOrder.UNSORTED;
        for (int i = 0; i < model.getColumnCount(); i++) {
            SortOrder sortOrder = entriesTable.getSortOrder(i);
            if(sortOrder != SortOrder.UNSORTED) {
                targetColumn = i;
                targetSortOrder = sortOrder;
            }
        }

        entriesTable.setModel(model);

        if(targetSortOrder != SortOrder.UNSORTED) {
            entriesTable.setSortOrder(targetColumn, targetSortOrder);
        }
        else {
            // Default sort order
            entriesTable.setSortOrder(0, SortOrder.ASCENDING);
        }

        updateBreadcrumb();

    }

    private void goUp() {
        if(currentDirectory != null && currentDirectory.getParent() != null) {
            setCurrentDirectory(currentDirectory.getParent());
        }
    }

    private void goNext() {
        if(history.canGoForward()) {
            Path next = history.goForward();
            if(next != null) {
                this.currentDirectory = next.toAbsolutePath();
                refresh();
            }
        }
    }

    private void goBack() {
        if(history.canGoBack()) {
            Path next = history.goBack();
            if(next != null) {
                this.currentDirectory = next.toAbsolutePath();
                refresh();
            }
        }
    }

    private void updateBreadcrumb() {
        breadcrumb.add(UIUtils.createJLabel("Test", 12));
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
