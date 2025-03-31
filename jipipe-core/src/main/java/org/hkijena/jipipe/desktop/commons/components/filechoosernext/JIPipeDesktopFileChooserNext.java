package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFancyTextField;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.FileChooserBookmark;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JIPipeDesktopFileChooserNext extends JPanel {

    public static final List<JIPipeDesktopFileChooserNextPathTypeMetadata> KNOWN_PATH_TYPES = new ArrayList<>();
    private static final int TOOLBAR_BUTTON_SIZE = 42;
    private final JIPipeDesktopFormPanel sidePanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JXTable entriesTable = new JXTable();
    private final JToolBar breadcrumbToolBar = new JToolBar();
    private final JIPipeDesktopFancyTextField breadcrumbPathEditor;
    private final JButton breadcrumbPathEditorConfirmButton;
    private final JIPipeWorkbench workbench;
    private final HTMLText description;
    private final Path initialDirectory;
    private final FileChooserHistory history = new FileChooserHistory();
    private final JCheckBoxMenuItem showHiddenToggle = new JCheckBoxMenuItem("Show hidden items");
    private final JPopupMenu entriesPopupMenu = new JPopupMenu();
    private final PathIOMode ioMode;
    private final PathType pathType;
    private final boolean multiple;
    private final List<FileNameExtensionFilter> extensionFilters = new ArrayList<>();
    private final JIPipeDesktopFancyTextField selectedPathEditor;
    private final JComboBox<FileNameExtensionFilter> extensionFilterComboBox = new JComboBox<>();
    private final JIPipeDesktopFormPanel bottomPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
    private final JIPipeDesktopFancyTextField textFilterEditor;
    private final JIPipeFileChooserApplicationSettings settings;
    private final JCheckBox autoAddExtension = new JCheckBox("Automatically add file extensions", true);
    boolean breadcrumbEditMode = false;
    private Path currentDirectory;
    private Consumer<List<Path>> callbackConfirm;


    public JIPipeDesktopFileChooserNext(JIPipeWorkbench workbench, HTMLText description, Path initialDirectory, PathIOMode ioMode, PathType pathType, boolean multiple, FileNameExtensionFilter... extensionFilters) {
        this.workbench = workbench;
        this.description = description;
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
        this.settings = JIPipe.isInstantiated() ? JIPipeFileChooserApplicationSettings.getInstance() : new JIPipeFileChooserApplicationSettings();

        this.extensionFilters.addAll(Arrays.asList(extensionFilters));
        this.extensionFilters.add(new FileNameExtensionFilter("All files", "*"));

        initialize();
        setCurrentDirectory(initialDirectory);
        refreshSidePanel();
    }

    public static Path showDialogSingle(Component parent, JIPipeWorkbench workbench, String title, HTMLText description, Path initialDirectory, PathIOMode ioMode, PathType pathType, FileNameExtensionFilter... extensionFilters) {
        List<Path> paths = showDialog(parent, workbench, title, description, initialDirectory, ioMode, pathType, false, extensionFilters);
        if (paths.isEmpty()) {
            return null;
        }
        return paths.get(0);
    }

    public static List<Path> showDialog(Component parent, JIPipeWorkbench workbench, String title, HTMLText description, Path initialDirectory, PathIOMode ioMode, PathType pathType, boolean multiple, FileNameExtensionFilter... extensionFilters) {
        JDialog dialog = new JDialog(parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        UIUtils.addEscapeListener(dialog);
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setIconImage(UIUtils.getJIPipeIcon128());

        JPanel mainPanel = new JPanel(new BorderLayout());

        JIPipeDesktopFileChooserNext panel = new JIPipeDesktopFileChooserNext(workbench, description, initialDirectory, ioMode, pathType, multiple, extensionFilters);
        mainPanel.add(panel, BorderLayout.CENTER);

        List<Path> result = new ArrayList<>();

        panel.setCallbackConfirm((paths) -> {
            result.addAll(paths);
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
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(parent);
        SwingUtilities.invokeLater(panel::refresh);
        if (pathType != PathType.DirectoriesOnly && ioMode == PathIOMode.Save) {
            SwingUtilities.invokeLater(panel::focusPathEditor);
        }
        dialog.setVisible(true);

        return result;
    }

    public static void main(String[] args) {

        registerKnownFileType("SVG image", UIUtils.getIconFromResources("mimetypes/svg.png"), ".svg");

        JIPipeDesktopUITheme.ModernLight.install();
        List<Path> paths = JIPipeDesktopFileChooserNext.showDialog(null, null, "Test dialog", HTMLText.EMPTY, Paths.get(""), PathIOMode.Open, PathType.FilesOnly, true, UIUtils.EXTENSION_FILTER_SVG);
        if (paths.isEmpty()) {
            System.out.println("No paths selected");
        } else {
            System.out.println(paths.stream().map(Object::toString).collect(Collectors.joining("; ")));
        }
        System.exit(0);
    }

    /**
     * Registers a new path type
     *
     * @param name            the name
     * @param icon            the icon. If 32x32, the icon itself is used. Otherwise, it is combined with the default file icon
     * @param extension       the extension (including dot)
     * @param otherExtensions other extensions (including dot)
     */
    public static void registerKnownFileType(String name, Icon icon, String extension, String... otherExtensions) {
        Set<String> extensions = new HashSet<>(Arrays.asList(otherExtensions));
        extensions.add(extension);

        Icon finalIcon;
        if (icon.getIconWidth() < 32) {
            // Create a combined icon
            finalIcon = new CompositeIcon(UIUtils.getIcon32FromResources("file.png"),
                    icon,
                    16 - icon.getIconWidth() / 2,
                    16 - icon.getIconHeight() / 2);
        } else {
            finalIcon = icon;
        }

        JIPipeDesktopFileChooserNextPathTypeMetadata metadata = new JIPipeDesktopFileChooserNextPathTypeMetadata(name, PathType.FilesOnly, extensions, finalIcon);
        KNOWN_PATH_TYPES.add(metadata);
    }

    /**
     * Registers a new path type
     *
     * @param name            the name
     * @param icon            the icon. If 32x32, the icon itself is used. Otherwise, it is combined with the default file icon
     * @param extension       the extension (including dot)
     * @param otherExtensions other extensions (including dot)
     */
    public static void registerKnownDirectoryType(String name, Icon icon, String extension, String... otherExtensions) {
        Set<String> extensions = new HashSet<>(Arrays.asList(otherExtensions));
        extensions.add(extension);

        Icon finalIcon;
        if (icon.getIconWidth() < 32) {
            // Create a combined icon
            finalIcon = new CompositeIcon(UIUtils.getIcon32FromResources("places/folder2-purple.png"),
                    icon,
                    16 - icon.getIconWidth() / 2,
                    19 - icon.getIconHeight() / 2);
        } else {
            finalIcon = icon;
        }

        JIPipeDesktopFileChooserNextPathTypeMetadata metadata = new JIPipeDesktopFileChooserNextPathTypeMetadata(name, PathType.DirectoriesOnly, extensions, finalIcon);
        KNOWN_PATH_TYPES.add(metadata);
    }

    private JLabel getPathTypeLabel(PathType pathType) {
        switch (pathType) {
            case FilesOnly:
                return new JLabel(UIUtils.getIconFromResources("data-types/file.png"));
            case DirectoriesOnly:
                return new JLabel(UIUtils.getIconFromResources("places/inode-directory.png"));
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
        createSidePanelHeader("Locations", UIUtils.makeButtonFlat25x25(UIUtils.createButton("", UIUtils.getIcon16FromResources("actions/view-refresh.png"), this::refreshSidePanel)));

        CommonDirectoriesProvider provider = new CommonDirectoriesProvider();
        for (CommonDirectory directory : provider.getStaticDirectories()) {
            createSidePanelShortcut(directory);
        }
        for (CommonDirectory directory : provider.getRefreshableDirectories()) {
            createSidePanelShortcut(directory);
        }

        createSidePanelHeader("Recent", UIUtils.makeButtonFlat25x25(UIUtils.createButton("", UIUtils.getIcon16FromResources("actions/view-refresh.png"), this::refreshSidePanel)));
        createSidePanelShortcut(PathUtils.getPathNameSafe(initialDirectory), UIUtils.getIconFromResources("actions/folder-open-recent.png"), initialDirectory);

        Set<Path> knownDirectories = new HashSet<>();
        knownDirectories.add(initialDirectory.toAbsolutePath());

        for (JIPipeFileChooserApplicationSettings.LastDirectoryKey directoryKey : JIPipeFileChooserApplicationSettings.LastDirectoryKey.values()) {
            Path directory = settings.getLastDirectoryBy(workbench, directoryKey);
            if (directory != null && Files.isDirectory(directory) && !StringUtils.isNullOrEmpty(directory)) {
                if (!knownDirectories.contains(directory)) {
                    createSidePanelShortcut(StringUtils.orElse(directory.getFileName(), "Root"), UIUtils.getIconFromResources("actions/folder-open-recent.png"), directory);
                    knownDirectories.add(directory);
                }
            }
        }

        if (workbench != null && workbench.getProject() != null) {
            List<Map.Entry<String, Path>> projectPaths = new ArrayList<>();
            JIPipeProject project = workbench.getProject();
            if (project.getWorkDirectory() != null && Files.isDirectory(project.getWorkDirectory())) {
                projectPaths.add(new ImmutablePair<>("Project directory", project.getWorkDirectory()));
            }
            for (Map.Entry<String, Path> entry : project.getDirectoryMap().entrySet()) {
                if (Files.isDirectory(entry.getValue())) {
                    projectPaths.add(new ImmutablePair<>(entry.getKey(), entry.getValue()));
                }
            }
            if (!projectPaths.isEmpty()) {
                createSidePanelHeader("Project", UIUtils.makeButtonFlat25x25(UIUtils.createButton("", UIUtils.getIcon16FromResources("actions/view-refresh.png"), this::refreshSidePanel)));
                for (Map.Entry<String, Path> projectPath : projectPaths) {
                    createSidePanelShortcut(projectPath.getKey(), UIUtils.getIconFromResources("actions/folder-open.png"), projectPath.getValue());
                }
            }
        }

        createSidePanelHeader("Bookmarks", UIUtils.makeButtonFlat25x25(UIUtils.createButton("", UIUtils.getIcon16FromResources("actions/add.png"), this::addBookmark)));
        for (FileChooserBookmark bookmark : settings.getBookmarks()) {
            JButton button = createSidePanelShortcut(bookmark.getName(), UIUtils.getIconFromResources("actions/bookmarks.png"), bookmark.getPath());
            JPopupMenu menu = UIUtils.addRightClickPopupMenuToButton(button);
            menu.add(UIUtils.createMenuItem("Rename", "Renames the entry", UIUtils.getIconFromResources("actions/accessories-text-editor.png"), () -> {
                String newName = JOptionPane.showInputDialog(this, "Set the new name of the entry", bookmark.getName());
                if (!StringUtils.isNullOrEmpty(newName)) {
                    bookmark.setName(newName);
                    refreshSidePanel();
                }
            }));
            menu.add(UIUtils.createMenuItem("Remove", "Removes the entry", UIUtils.getIconFromResources("actions/gtk-delete.png"), () -> {
                settings.getBookmarks().remove(bookmark);
                refreshSidePanel();
            }));
        }


    }

    private void addBookmark() {
        String name = JOptionPane.showInputDialog(this, "Enter the name of the bookmark", StringUtils.orElse(currentDirectory.getFileName(), "Root"));
        if (!StringUtils.isNullOrEmpty(name)) {
            settings.getBookmarks().add(new FileChooserBookmark(name, currentDirectory));
            if (JIPipe.isInstantiated()) {
                JIPipe.getSettings().saveLater();
            }
            refreshSidePanel();
        }
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
                if (SystemUtils.IS_OS_WINDOWS) {
                    name = directory.getPath().toString();
                } else {
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

    private JButton createSidePanelShortcut(String name, Icon icon, Path path) {
        JButton button = new JButton(name, icon);
        button.setHorizontalAlignment(JButton.LEFT);
        button.addActionListener(e -> {
            setCurrentDirectory(path);
        });
        UIUtils.makeButtonFlat(button);
        sidePanel.addWideToForm(button);
        return button;
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

        if (!multiple) {
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
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    navigateSelected();
                } else if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1) {
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
                if (!breadcrumbEditMode) {
                    breadcrumbEditMode = true;
                    updateBreadcrumb();
                }
            }
        });
        breadcrumbPathEditor.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmBreadcrumbPathEditor();
                }
            }
        });

        selectedPathEditor.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    doCallbackConfirmIfValid();
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
        if (selectedRows != null) {
            for (int selectedRow : selectedRows) {
                int modelRow = entriesTable.convertRowIndexToModel(selectedRow);
                Path selectedPath = (Path) entriesTable.getModel().getValueAt(modelRow, 0);

                // Filter based on settings
                if (pathType == PathType.DirectoriesOnly) {
                    if (Files.isDirectory(selectedPath)) {
                        result.add(selectedPath);
                    }
                } else if (pathType == PathType.FilesOnly) {
                    if (!Files.isDirectory(selectedPath)) {
                        result.add(selectedPath);
                    }
                } else {
                    result.add(selectedPath);
                }
            }
        }
        return result;
    }

    private void updatePathEditorBySelection() {
        List<Path> selectedPathsInTable = getSelectedPathsInTable();
        if (selectedPathsInTable.size() == 1) {
            selectedPathEditor.setText(StringUtils.nullToEmpty(selectedPathsInTable.get(0).getFileName()));
        } else {
            selectedPathEditor.setText("");
        }
    }

    private void initializeBottomPanel() {

        if (description != null && !description.toPlainText().isEmpty()) {
            JTextPane textPane = UIUtils.createBorderlessReadonlyTextPane(description.getHtml(), true);
            textPane.setBackground(JIPipeDesktopMessagePanel.MessageType.InfoLight.getBackground());
            textPane.setBorder(
                    BorderFactory.createCompoundBorder(
                            new RoundedLineBorder(JIPipeDesktopMessagePanel.MessageType.InfoLight.getBackground().darker(), 1, 4),
                            UIUtils.createEmptyBorder(8)
                    ));
            bottomPanel.addWideToForm(textPane);
        }

        selectedPathEditor.setMinimumSize(new Dimension(100, 42));
        selectedPathEditor.setPreferredSize(new Dimension(100, 42));
        extensionFilterComboBox.setMinimumSize(new Dimension(100, 42));
        extensionFilterComboBox.setPreferredSize(new Dimension(100, 42));

        if (pathType == PathType.FilesOnly) {

            bottomPanel.addToForm(selectedPathEditor, new JLabel("Name"));
        } else {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(selectedPathEditor, BorderLayout.CENTER);
            panel.add(UIUtils.createButton("Use current directory", UIUtils.getIconFromResources("actions/edit-paste.png"), () -> {
                selectedPathEditor.setText(currentDirectory.toAbsolutePath().toString());
            }), BorderLayout.EAST);
            bottomPanel.addToForm(panel, new JLabel("Name"));
        }

        if (pathType == PathType.FilesOnly || pathType == PathType.FilesAndDirectories) {
            // Add the filter combo box
            bottomPanel.addToForm(extensionFilterComboBox, new JLabel("Filter"));

            // Add auto extensions
            if (ioMode == PathIOMode.Save) {
                bottomPanel.addWideToForm(autoAddExtension);
            }
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

        if (path != null) {
            entriesPopupMenu.add(UIUtils.createMenuItem("Open in default application", "Opens the selected item in the system-wide default application", UIUtils.getIconFromResources("actions/fileopen.png"), () -> {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.open(path.toFile());
                } catch (Exception ex) {
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
        } catch (Exception ex) {
        }
    }

    private void navigateSelected() {
        int selectedRow = entriesTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = entriesTable.convertRowIndexToModel(selectedRow);
            Path path = (Path) entriesTable.getModel().getValueAt(modelRow, 0);

            if (Files.isDirectory(path)) {
                // Directories -> set as current directory
                setCurrentDirectory(path);
            } else {
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

        showHiddenToggle.addActionListener(e -> {
            refreshFilter();
        });
    }

    private void refreshFilter() {
        entriesTable.setRowFilter(new JIPipeDesktopFileChooserNextTableFilter((FileNameExtensionFilter) extensionFilterComboBox.getSelectedItem(), textFilterEditor.getText()));
    }

    private void createDirectory() {
        String name = JOptionPane.showInputDialog(this, "Please enter the name of the newly created directory");
        if (!StringUtils.isNullOrEmpty(name)) {
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
                String name;
                if (current.getFileName() != null) {
                    name = current.getFileName().toString();
                } else {
                    name = current.toString();
                }

                JButton navigateButton = new JButton(StringUtils.orElse(name, "/"));
                navigateButton.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                navigateButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                JButton listChildrenButton = new JButton(UIUtils.getIcon12FromResources("actions/go-right.png"));
                listChildrenButton.setBorder(BorderFactory.createEmptyBorder(6, 3, 6, 3));
                listChildrenButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                int segmentWidth = navigateButton.getFontMetrics(navigateButton.getFont()).stringWidth(navigateButton.getText()) + 6 + 12 + 6;
                fullSegmentWidth += segmentWidth;

                if ((fullSegmentWidth + 64) >= breadcrumbToolBar.getWidth() && overhangMenu == null) {
                    // Create a navigation menu that will be used instead
                    breadcrumbToolBar.add(listChildrenButton, 0);
                    overhangMenu = new JPopupMenu();
                    UIUtils.addPopupMenuToButton(listChildrenButton, overhangMenu);
                }

                Path finalCurrent = current;

                if (overhangMenu == null) {
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
                                } catch (Exception ex) {
                                }
                            });
                        } catch (Exception ex) {
                        }
                    });
                } else {
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

    private void focusPathEditor() {
        selectedPathEditor.getTextField().requestFocusInWindow();
    }

    public void doCallbackConfirmIfValid() {

        // Special case if File dialog: If path is directory, then navigate there
        if (pathType == PathType.FilesOnly) {
            try {
                if (!StringUtils.isNullOrEmpty(selectedPathEditor.getText())) {
                    Path path = Paths.get(selectedPathEditor.getText());
                    if (path.isAbsolute() && Files.isDirectory(path)) {
                        setCurrentDirectory(path);
                        selectedPathEditor.setText("");
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        List<Path> selectedPaths = getSelectedPaths();
        if (!selectedPaths.isEmpty()) {
            if (callbackConfirm != null) {
                callbackConfirm.accept(selectedPaths);
            }
        }
    }

    private Path getSelectedPathInEditor() {
        if (!StringUtils.isNullOrEmpty(selectedPathEditor.getText())) {
            Path selectedPath = Paths.get(selectedPathEditor.getText());
            if (!selectedPath.isAbsolute()) {
                // Is a name
                selectedPath = currentDirectory.resolve(selectedPath);
            }
            return selectedPath;
        }
        return null;
    }

    public List<Path> getSelectedPaths() {
        List<Path> results = new ArrayList<>();

        if (ioMode == PathIOMode.Open) {
            // We first try to handle the path editor
            Path editorPath = getSelectedPathInEditor();
            if (editorPath != null) {
                try {
                    // Must exist and be of the correct type
                    if (Files.exists(editorPath)) {
                        if (pathType == PathType.DirectoriesOnly) {
                            if (Files.isDirectory(editorPath)) {
                                results.add(editorPath);
                            }
                        } else if (pathType == PathType.FilesOnly) {
                            if (!Files.isDirectory(editorPath)) {
                                results.add(editorPath);
                            }
                        } else {
                            results.add(editorPath);
                        }
                    }
                } catch (Throwable ignored) {
                }
            } else if (pathType == PathType.DirectoriesOnly) {
                results.add(getCurrentDirectory());
            }
        } else {
            Path editorPath = getSelectedPathInEditor();
            if (editorPath != null) {
                if (pathType == PathType.DirectoriesOnly) {
                    // Create the directory and return it
                    try {
                        Files.createDirectories(editorPath);
                        results.add(editorPath);
                    } catch (Throwable ignored) {
                        JOptionPane.showMessageDialog(this,
                                "Unable to create directory '" + editorPath + "'!",
                                "Save directory",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // Add the extension (if requested)
                    if (autoAddExtension.isSelected()) {
                        Object filter = extensionFilterComboBox.getSelectedItem();
                        String fileName = editorPath.getFileName().toString();
                        if (filter instanceof FileNameExtensionFilter) {
                            boolean anyMatch = false;
                            for (String extension : ((FileNameExtensionFilter) filter).getExtensions()) {
                                if (!StringUtils.isNullOrEmpty(extension) && !"*".equals(extension)) {
                                    if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
                                        anyMatch = true;
                                        break;
                                    }
                                }
                            }

                            if (!anyMatch) {
                                // Add the first valid extension
                                for (String extension : ((FileNameExtensionFilter) filter).getExtensions()) {
                                    if (!StringUtils.isNullOrEmpty(extension) && !"*".equals(extension)) {
                                        editorPath = editorPath.getParent().resolve(fileName + "." + extension);
                                    }
                                }
                            }
                        }
                    }

                    results.add(editorPath);
                }
            }
        }

        if (results.isEmpty()) {
            // Use the paths from the selection within the table
            results.addAll(getSelectedPathsInTable());
        }

        // Removed for consistency
//        if (ioMode == PathIOMode.Save && pathType != PathType.DirectoriesOnly) {
//            // Check if files already exist
//            boolean foundExisting = false;
//
//            for (Path path : results) {
//                if (!Files.isDirectory(path) && Files.exists(path)) {
//                    foundExisting = true;
//                }
//            }
//
//            if (foundExisting) {
//                if (JOptionPane.showConfirmDialog(this, "The selected file(s) already exist. Overwrite them?", "Save file", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
//                    results.clear();
//                }
//            }
//
//        }

        return results;
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(Path currentDirectory) {
        this.breadcrumbEditMode = false;
        this.currentDirectory = currentDirectory.toAbsolutePath();
        this.history.insert(currentDirectory);
        refresh();
    }

    public void doCallbackConfirm() {
        if (callbackConfirm != null) {
            callbackConfirm.accept(getSelectedPaths());
        }
    }

    public Consumer<List<Path>> getCallbackConfirm() {
        return callbackConfirm;
    }

    public void setCallbackConfirm(Consumer<List<Path>> callbackConfirm) {
        this.callbackConfirm = callbackConfirm;
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
