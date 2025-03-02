package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.apache.commons.lang.SystemUtils;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFancyTextField;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.utils.*;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JIPipeDesktopFileChooserNext extends JPanel {

    private static final int TOOLBAR_BUTTON_SIZE = 42;
    private final JIPipeDesktopFormPanel sidePanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JXTable entriesTable = new JXTable();
    private final JToolBar breadcrumbToolBar = new JToolBar();
    private final JIPipeDesktopFancyTextField breadcrumbPathEditor;
    private final JButton breadcrumbPathEditorConfirmButton;
    private final Path initialDirectory;
    private Path currentDirectory;
    private final FileChooserHistory history = new FileChooserHistory();
    boolean breadcrumbEditMode = false;
    private final JCheckBoxMenuItem showHiddenToggle = new JCheckBoxMenuItem("Show hidden items");
    private final JPopupMenu entriesPopupMenu = new JPopupMenu();
    private final PathIOMode ioMode;
    private final PathType pathType;
    private final boolean multiple;
    private final List<FileNameExtensionFilter> extensionFilters = new ArrayList<>();
    private final JIPipeDesktopFancyTextField selectedPathEditor;
    private final JComboBox<FileNameExtensionFilter> extensionFilterComboBox = new JComboBox<>();
    private final JIPipeDesktopFormPanel bottomPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
    private Consumer<List<Path>> callbackConfirm;
    private final JIPipeDesktopFancyTextField textFilterEditor;


    public JIPipeDesktopFileChooserNext(Path initialDirectory, PathIOMode ioMode, PathType pathType, boolean multiple, FileNameExtensionFilter... extensionFilters) {
        this.initialDirectory = initialDirectory.toAbsolutePath();
        this.ioMode = ioMode;
        this.pathType = pathType;
        this.multiple = multiple;
        this.breadcrumbPathEditor = new JIPipeDesktopFancyTextField(new JLabel(UIUtils.getIconFromResources("places/inode-directory.png")),
                "Enter the directory here", false);
        this.textFilterEditor = new JIPipeDesktopFancyTextField(new JLabel(UIUtils.getIconFromResources("actions/view-filter.png")), "Filter ...", true);
        this.textFilterEditor.addActionListener(e -> refreshFilter());
        this.selectedPathEditor = new JIPipeDesktopFancyTextField(getPathTypeLabel(pathType), "", false);
        this.breadcrumbPathEditorConfirmButton = UIUtils.createButton("",
                UIUtils.getIconFromResources("actions/check.png"), this::confirmBreadcrumbPathEditor);
        UIUtils.makeButtonFlatWithSize(breadcrumbPathEditorConfirmButton, TOOLBAR_BUTTON_SIZE);

        this.extensionFilters.addAll(Arrays.asList(extensionFilters));
        this.extensionFilters.add(new FileNameExtensionFilter("All files", "*"));

        initialize();
        setCurrentDirectory(initialDirectory);
        refreshSidePanel();
    }

    private JLabel getPathTypeLabel(PathType pathType) {
        switch (pathType) {
            case FilesOnly:
                return new JLabel(UIUtils.getIconFromResources("data-types/file.png"));
            case DirectoriesOnly:
                return new JLabel(UIUtils.getIconFromResources("data-types/folder.png"));
            default:
                return new JLabel(UIUtils.getIconFromResources("data-types/path.png"));
        }
    }

    public PathIOMode getIoMode() {
        return ioMode;
    }

    public PathType getPathType() {
        return pathType;
    }

    private void refreshSidePanel() {
        sidePanel.clear();
        createSidePanelHeader("Locations", UIUtils.makeButtonFlat25x25(UIUtils.createButton("", UIUtils.getIconInvertedFromResources("actions/view-refresh.png"), this::refreshSidePanel)));

        CommonDirectoriesProvider provider = new CommonDirectoriesProvider();
        for (CommonDirectory directory : provider.getStaticDirectories()) {
            createSidePanelShortcut(directory);
        }
        for (CommonDirectory directory : provider.getRefreshableDirectories()) {
            createSidePanelShortcut(directory);
        }

        createSidePanelHeader("Recent");
        createSidePanelShortcut(PathUtils.getPathNameSafe(initialDirectory), UIUtils.getIconFromResources("actions/folder-open-recent.png"), initialDirectory);
    }

    private void createSidePanelShortcut(CommonDirectory directory) {
        String name;
        Icon icon;
        switch (directory.getType()) {
            case HOME: {
                name = "Home";
                icon = UIUtils.getIconFromResources("places/user-home.png");
            }
            break;
            case ROOT: {
                name = "Root";
                icon = UIUtils.getIconFromResources("places/folder-root.png");
            }
            break;
            case DRIVE: {
                if(SystemUtils.IS_OS_WINDOWS) {
                    name = directory.getPath().toString();
                }
                else {
                    name = directory.getPath().getFileName().toString();
                }
                icon = UIUtils.getIconFromResources("devices/drive-harddisk.png");
            }
            break;
            default: {
                name = directory.getPath().getFileName().toString();
                icon = UIUtils.getIconFromResources("places/inode-directory.png");
            }
        }

        Path path = directory.getPath();
        createSidePanelShortcut(name, icon, path);
    }

    private void createSidePanelShortcut(String name, Icon icon, Path path) {
        JButton button = new JButton(name, icon);
        button.setHorizontalAlignment(JButton.LEFT);
        button.addActionListener(e -> { setCurrentDirectory(path); });
        UIUtils.makeButtonFlat(button);
        sidePanel.addWideToForm(button);
    }

    private void createSidePanelHeader(String title, JComponent... components) {
        JPanel panel = UIUtils.boxHorizontal(components);
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11));
        label.setForeground(UIUtils.getIconBaseColor());
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(UIUtils.createHorizontalFillingSeparator(), 0);
        panel.add(label, 0);
        sidePanel.addWideToForm(panel);
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

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        initializeBottomPanel();

        // File name filter
        extensionFilterComboBox.setModel(UIUtils.toComboBoxModel(extensionFilters));
        extensionFilterComboBox.setRenderer(new FileNameExtensionsFilterListCellRenderer());
        extensionFilterComboBox.addActionListener(e -> refresh());

        // Entries table
        entriesTable.setModel(new JIPipeDesktopFileChooserNextTableModel(null, false, false));

        entriesTable.setShowGrid(false);
        entriesTable.setHighlighters(
                HighlighterFactory.createSimpleStriping(UIManager.getColor("Panel.background"))
        );

        if(!multiple) {
            entriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

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
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    navigateSelected();
                }
                else if(SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1) {
                    int row = entriesTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && !entriesTable.isRowSelected(row)) {
                        entriesTable.getSelectionModel().setSelectionInterval(row, row);  // Select row under mouse
                    }
                    openContextMenu(e);
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
        entriesTable.getSelectionModel().addListSelectionListener(e -> {
           updatePathEditorBySelection();
        });

        // Breadcrumb and toolbar
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
                sidePanel,
                mainPanel,
                new JIPipeDesktopSplitPane.DynamicSidebarRatio(200, true));
        add(splitPane, BorderLayout.CENTER);
    }

    private List<Path> getSelectedPathsInTable() {
        List<Path> result = new ArrayList<>();
        int[] selectedRows = entriesTable.getSelectedRows();
        if(selectedRows != null) {
            for (int selectedRow : selectedRows) {
                int modelRow = entriesTable.convertRowIndexToModel(selectedRow);
                Path selectedPath = (Path) entriesTable.getModel().getValueAt(modelRow, 0);

                // Filter based on settings
                if(pathType == PathType.DirectoriesOnly) {
                    if(Files.isDirectory(selectedPath)) {
                        result.add(selectedPath);
                    }
                }
                else if(pathType == PathType.FilesOnly) {
                    if(!Files.isDirectory(selectedPath)) {
                        result.add(selectedPath);
                    }
                }
                else {
                    result.add(selectedPath);
                }
            }
        }
        return result;
    }

    private void updatePathEditorBySelection() {
        List<Path> selectedPathsInTable = getSelectedPathsInTable();
        if(selectedPathsInTable.size() == 1) {
            selectedPathEditor.setText(StringUtils.nullToEmpty(selectedPathsInTable.get(0).getFileName()));
        }
        else {
            selectedPathEditor.setText("");
        }
    }

    private void initializeBottomPanel() {

        selectedPathEditor.setMinimumSize(new Dimension(100, 42));
        selectedPathEditor.setPreferredSize(new Dimension(100, 42));
        extensionFilterComboBox.setMinimumSize(new Dimension(100, 42));
        extensionFilterComboBox.setPreferredSize(new Dimension(100, 42));

        if(pathType == PathType.FilesOnly) {

            bottomPanel.addToForm(selectedPathEditor, new JLabel("Name"));
        }
        else {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(selectedPathEditor, BorderLayout.CENTER);
            panel.add(UIUtils.createButton("Use current directory", UIUtils.getIconFromResources("actions/edit-paste.png"), () -> {
                selectedPathEditor.setText(currentDirectory.toAbsolutePath().toString());
            }), BorderLayout.EAST);
            bottomPanel.addToForm(panel, new JLabel("Name"));
        }

        if(pathType == PathType.FilesOnly || pathType == PathType.FilesAndDirectories) {
            // Add the filter combo box
            bottomPanel.addToForm(extensionFilterComboBox, new JLabel("Filter"));
        }
    }

    private void openContextMenu(MouseEvent e) {
        Path path;
        int selectedRow = entriesTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = entriesTable.convertRowIndexToModel(selectedRow);
            path = (Path) entriesTable.getModel().getValueAt(modelRow, 0);
        } else {
            path = null;
        }

        entriesPopupMenu.removeAll();

        if(path != null) {
            entriesPopupMenu.add(UIUtils.createMenuItem("Open in default application", "Opens the selected item in the system-wide default application", UIUtils.getIconFromResources("actions/fileopen.png"), () -> {
               try {
                   Desktop desktop = Desktop.getDesktop();
                   desktop.open(path.toFile());
               }
               catch (Exception ex) {
               }
            }));
            entriesPopupMenu.add(UIUtils.createMenuItem("Copy path", "Copies the full path to the clipboard", UIUtils.getIconFromResources("actions/url-copy.png"), () -> {
                UIUtils.copyToClipboard(path.toString());
            }));
        }

        UIUtils.addSeparatorIfNeeded(entriesPopupMenu);
        entriesPopupMenu.add(UIUtils.createMenuItem("Open current directory", "Opens the current directory in the system file manager", UIUtils.getIconFromResources("actions/fileopen.png"), this::openCurrentDirectoryInSystem));
        entriesPopupMenu.add(UIUtils.createMenuItem("Refresh", "Refreshes the view", UIUtils.getIconFromResources("actions/view-refresh.png"), this::refresh));

        entriesPopupMenu.show(entriesTable, e.getX(), e.getY());
    }

    private void openCurrentDirectoryInSystem() {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.open(currentDirectory.toFile());
        }
        catch (Exception ex) {}
    }

    private void navigateSelected() {
        int selectedRow = entriesTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = entriesTable.convertRowIndexToModel(selectedRow);
            Path path = (Path) entriesTable.getModel().getValueAt(modelRow, 0);

            if (Files.isDirectory(path)) {
                // Directories -> set as current directory
                setCurrentDirectory(path);
            }
            else {
                // Files -> confirm
                selectedPathEditor.setText("");
                doCallbackConfirm();
            }
        }
    }

    private void initializeToolbar(JToolBar toolbar) {
        toolbar.add(UIUtils.makeButtonFlatWithSize(UIUtils.createButton("", UIUtils.getIconFromResources("actions/back.png"), this::goBack), TOOLBAR_BUTTON_SIZE));
        toolbar.add(UIUtils.makeButtonFlatWithSize(UIUtils.createButton("", UIUtils.getIconFromResources("actions/next.png"), this::goNext), TOOLBAR_BUTTON_SIZE));
        toolbar.add(UIUtils.makeButtonFlatWithSize(UIUtils.createButton("", UIUtils.getIconFromResources("actions/up.png"), this::goUp), TOOLBAR_BUTTON_SIZE));
        toolbar.add(UIUtils.makeButtonFlatWithSize(UIUtils.createButton("", UIUtils.getIconFromResources("actions/view-refresh.png"), this::refresh), TOOLBAR_BUTTON_SIZE));

        toolbar.add(Box.createHorizontalGlue());
        textFilterEditor.setMaximumSize(new Dimension(200, 42));
        textFilterEditor.setPreferredSize(new Dimension(200, 42));
        toolbar.add(textFilterEditor);
        toolbar.add(Box.createHorizontalGlue());

        toolbar.add(UIUtils.makeButtonFlat(UIUtils.createButton("New directory", UIUtils.getIconFromResources("actions/archive-insert-directory.png"), this::createDirectory)));

        JButton menuButton = UIUtils.makeButtonFlatWithSize(new JButton(UIUtils.getIcon16FromResources("actions/hamburger-menu.png")), TOOLBAR_BUTTON_SIZE);
        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(menuButton);
        popupMenu.add(showHiddenToggle);
        toolbar.add(menuButton);

        showHiddenToggle.addActionListener(e -> { refreshFilter(); });
    }

    private void refreshFilter() {
        entriesTable.setRowFilter(new JIPipeDesktopFileChooserNextTableFilter((FileNameExtensionFilter) extensionFilterComboBox.getSelectedItem(), textFilterEditor.getText()));
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
        JIPipeDesktopFileChooserNextTableModel model = new JIPipeDesktopFileChooserNextTableModel(currentDirectory,
                pathType != PathType.DirectoriesOnly, showHiddenToggle.isSelected());

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
        refreshFilter();

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

    public static List<Path> showDialog(JComponent parent, String title, Path initialDirectory, PathIOMode ioMode, PathType pathType, boolean multiple, FileNameExtensionFilter... extensionFilters) {
        JDialog dialog = new JDialog(parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        UIUtils.addEscapeListener(dialog);
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setIconImage(UIUtils.getJIPipeIcon128());

        JPanel mainPanel = new JPanel(new BorderLayout());

        JIPipeDesktopFileChooserNext panel = new JIPipeDesktopFileChooserNext(initialDirectory, ioMode, pathType, multiple, extensionFilters);
        mainPanel.add(panel, BorderLayout.CENTER);

        List<Path>[] callback = new List[1];

        panel.setCallbackConfirm((paths) -> {
            callback[0] = paths;
            dialog.setVisible(false);
        });

        JPanel buttonPanel = UIUtils.boxHorizontal(
                Box.createHorizontalGlue(),
                UIUtils.createButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"), () -> dialog.setVisible(false)),
                UIUtils.createButton(ioMode.name(), UIUtils.getIconFromResources("actions/check.png"), panel::doCallbackConfirmIfValid)
        );
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
        panel.bottomPanel.addWideToForm(buttonPanel);

        dialog.setContentPane(mainPanel);
        dialog.setLocationRelativeTo(parent);
        dialog.pack();
        dialog.setSize(1024, 768);
        SwingUtilities.invokeLater(panel::refresh);
        dialog.setVisible(true);

        return callback[0];
    }

    public void doCallbackConfirmIfValid() {
        if(!getSelectedPaths().isEmpty()) {
            doCallbackConfirm();
        }
    }

    private List<Path> getSelectedPaths() {
        List<Path> results = new ArrayList<>();

        // We first try to handle the path editor
        if(!StringUtils.isNullOrEmpty(selectedPathEditor.getText())) {
            try {
                Path selectedPath = Paths.get(selectedPathEditor.getText());
                if(!selectedPath.isAbsolute()) {
                    // Is a name
                    selectedPath = currentDirectory.resolve(selectedPath);
                }

                if(ioMode == PathIOMode.Save) {
                    // Accept as-is
                    results.add(selectedPath);
                }
                else {
                    // Must exist and be of the correct type
                    if(Files.exists(selectedPath)) {
                        if(pathType == PathType.DirectoriesOnly) {
                            if(Files.isDirectory(selectedPath)) {
                                results.add(selectedPath);
                            }
                        }
                        else if(pathType == PathType.FilesOnly) {
                            if(!Files.isDirectory(selectedPath)) {
                                results.add(selectedPath);
                            }
                        }
                        else {
                            results.add(selectedPath);
                        }
                    }
                }
            }
            catch (Throwable ignored) {}
        }
        else if(pathType == PathType.DirectoriesOnly) {
            results.add(getCurrentDirectory());
        }

        if(results.isEmpty()) {
            // Use the paths from the selection within the table
            results.addAll(getSelectedPathsInTable());
        }

        return results;
    }

    public static void main(String[] args) {
        JIPipeDesktopUITheme.ModernLight.install();
        List<Path> paths = JIPipeDesktopFileChooserNext.showDialog(null, "Test dialog", Paths.get(""), PathIOMode.Open, PathType.FilesOnly, true, UIUtils.EXTENSION_FILTER_SVG);
        if(paths == null) {
            System.out.println("No paths selected");
        }
        else {
            System.out.println(paths.stream().map(Object::toString).collect(Collectors.joining("; ")));
        }
        System.exit(0);
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(Path currentDirectory) {
        this.currentDirectory = currentDirectory.toAbsolutePath();
        this.history.insert(currentDirectory);
        refresh();
    }

    public void doCallbackConfirm() {
        if(callbackConfirm != null) {
            callbackConfirm.accept(getSelectedPaths());
        }
    }

    public  Consumer<List<Path>> getCallbackConfirm() {
        return callbackConfirm;
    }

    public void setCallbackConfirm( Consumer<List<Path>> callbackConfirm) {
        this.callbackConfirm = callbackConfirm;
    }
}
