package org.hkijena.jipipe.ui.components;

import ij.Prefs;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import sun.awt.shell.ShellFolder;
import sun.swing.FilePane;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.metal.MetalFileChooserUI;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AdvancedFileChooser extends JPanel implements PropertyChangeListener, ActionListener {

    private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private final FileChooserComponent fileChooserComponent = new FileChooserComponent();
    private FormPanel linkPanel;
    private List<Path> history = new ArrayList<>();
    private int currentHistoryIndex = 0;
    private Path initialDirectory;
    private final JToolBar drillDownToolBar = new JToolBar();
    private boolean drillDownEditMode = false;
    private FancyTextField pathField;
    private JToggleButton bookmarkToggle;

    private JDialog dialog = null;
    private int dialogType = JFileChooser.OPEN_DIALOG;
    private int returnValue = JFileChooser.ERROR_OPTION;

    public AdvancedFileChooser() {
        this(Paths.get("").toAbsolutePath().toFile());
    }

    public AdvancedFileChooser(File initialDirectory) {
        initialize();
        updateLinks();
        fileChooserComponent.addPropertyChangeListener(this);
        fileChooserComponent.addActionListener(this);
        setInitialDirectory(initialDirectory.toPath());

        // Update drilldown on component resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if(!drillDownEditMode) {
                    updateDrillDown();
                }
            }
        });
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new BorderLayout());
        linkPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                linkPanel,
                contentPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        add(splitPane, BorderLayout.CENTER);

        JPanel toolBarPanel = new JPanel(new BorderLayout());
        JToolBar actionToolBar = new JToolBar();
        toolBarPanel.add(actionToolBar, BorderLayout.NORTH);
        toolBarPanel.add(drillDownToolBar, BorderLayout.SOUTH);
        contentPanel.add(toolBarPanel, BorderLayout.NORTH);

        contentPanel.add(fileChooserComponent, BorderLayout.CENTER);
        initializeActionToolbar(actionToolBar);

        pathField = new FancyTextField(new JLabel(UIUtils.getIconFromResources("places/inode-directory.png")),
                "Enter the directory here", true);
        pathField.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    drillDownEditMode = false;
                    trySetCurrentDirectory(pathField.getText());
                }
                super.keyReleased(e);
            }
        });
        drillDownToolBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(!drillDownEditMode) {
                    switchToDrillDownEditMode();
                }
                else {
                    super.mouseClicked(e);
                }
            }
        });
    }

    /**
     * Gets the underlying file chooser component
     * @return the filter chooser
     */
    public FileChooserComponent getFileChooserComponent() {
        return fileChooserComponent;
    }

    private void switchToDrillDownEditMode() {
        drillDownEditMode = true;
        updateDrillDown();
        SwingUtilities.invokeLater(() -> pathField.getTextField().requestFocusInWindow());
    }

    private void updateDrillDown() {
        drillDownToolBar.removeAll();
        if(drillDownEditMode) {
            createDrillDownEditor();
        }
        else {
            createClickableDrillDown();
        }
        drillDownToolBar.revalidate();
        drillDownToolBar.repaint();
    }

    private void createDrillDownEditor() {
        drillDownToolBar.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        String pathString = fileChooserComponent.getCurrentDirectory().getAbsolutePath();
        if(!pathString.endsWith("/") && !pathString.endsWith("/")) {
            pathString = pathString + File.separator;
        }
        pathField.setText(pathString);
        drillDownToolBar.setLayout(new BorderLayout());
        drillDownToolBar.add(pathField, BorderLayout.CENTER);

        JButton acceptButton = new JButton(UIUtils.getIconFromResources("actions/ok.png"));
        acceptButton.setToolTipText("Navigate to selected path");
        acceptButton.addActionListener(e -> {
            drillDownEditMode = false;
            trySetCurrentDirectory(pathField.getText());
        });
        UIUtils.makeFlat25x25(acceptButton);
        drillDownToolBar.add(acceptButton, BorderLayout.EAST);
    }

    private void trySetCurrentDirectory(String path) {
        try {
            Path p = Paths.get(path);
            if(!Files.isDirectory(p)) {
                p = p.getParent();
            }
            if(Files.isDirectory(p)) {
                fileChooserComponent.setCurrentDirectory(p.toFile());
            }
        }
        catch (Exception e) {
        }
        finally {
            updateDrillDown();
        }
    }

    private void createClickableDrillDown() {
        drillDownToolBar.setLayout(new BoxLayout(drillDownToolBar, BoxLayout.X_AXIS));
        drillDownToolBar.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        Path currentDirectory = fileChooserComponent.getCurrentDirectory().toPath();
        List<Path> parents = new ArrayList<>();
        List<Path> removedParents = new ArrayList<>();
        parents.add(currentDirectory);
        while(currentDirectory.getParent() != null && !currentDirectory.getParent().equals(currentDirectory)) {
            parents.add(currentDirectory.getParent());
            currentDirectory = currentDirectory.getParent();
        }

        int availableWidth = drillDownToolBar.getWidth() - 75;
        FontMetrics metrics = getFontMetrics(getFont());
        for (int i = 1; i < parents.size(); ++i) {
            if(availableWidth > 0) {
                Path path = parents.get(i);
                String name;
                if (path.getFileName() != null)
                    name = path.getFileName().toString();
                else
                    name = path.toString();
                int componentWidth = 4 + 2 + 4 + 2 + 16 + metrics.stringWidth(name) + 32;
                availableWidth -= componentWidth;
            }
            if(availableWidth <= 0) {
                removedParents.add(parents.get(i));
                parents.remove(i);
                --i;
            }
        }

        if(!removedParents.isEmpty()) {
            JButton pathButton = new JButton("...");
            pathButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            pathButton.setBorder(BorderFactory.createEmptyBorder(8,4,8,2));
            drillDownToolBar.add(pathButton);
            JButton nextButton = new JButton(UIUtils.getIconFromResources("actions/arrow-right.png"));
            nextButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            nextButton.setBorder(BorderFactory.createEmptyBorder(8,2,8,4));
            drillDownToolBar.add(nextButton);

            JPopupMenu menu = new JPopupMenu();
            for (Path path : removedParents) {
                String name;
                if (path.getFileName() != null)
                    name = path.getFileName().toString();
                else
                    name = path.toString();
                JMenuItem pathItem = new JMenuItem(name);
                pathItem.setToolTipText(path.toString());
                pathItem.addActionListener(e -> fileChooserComponent.setCurrentDirectory(path.toFile()));
                menu.add(pathItem);
            }

            UIUtils.addPopupMenuToComponent(pathButton, menu);
            UIUtils.addPopupMenuToComponent(nextButton, menu);
        }

        Collections.reverse(parents);
        for (Path path : parents) {
            String name;
            if(path.getFileName() != null)
                name = path.getFileName().toString();
            else
                name = path.toString();
            JButton pathButton = new JButton(name);
            pathButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            pathButton.setBorder(BorderFactory.createEmptyBorder(8,4,8,2));
            drillDownToolBar.add(pathButton);
            pathButton.addActionListener(e -> fileChooserComponent.setCurrentDirectory(path.toFile()));

            JButton nextButton = new JButton(UIUtils.getIconFromResources("actions/arrow-right.png"));
            nextButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            nextButton.setBorder(BorderFactory.createEmptyBorder(8,2,8,4));
            drillDownToolBar.add(nextButton);

            JPopupMenu nextMenu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToComponent(nextButton, nextMenu, () -> {
                nextMenu.removeAll();
                try {
                    Files.list(path).forEach(child -> {
                        if(Files.isDirectory(child)) {
                            JMenuItem childItem = new JMenuItem(StringUtils.orElse(child.getFileName(), child.toString()));
                            childItem.addActionListener(e -> fileChooserComponent.setCurrentDirectory(child.toFile()));
                            nextMenu.add(childItem);
                        }
                    });
                } catch (IOException e) {
                    JMenuItem errorItem = new JMenuItem("Could not list directories");
                    errorItem.setEnabled(false);
                    nextMenu.add(errorItem);
                }
            });
        }
    }

    private void initializeActionToolbar(JToolBar actionToolbar) {
        JButton goBackButton = new JButton(UIUtils.getIconFromResources("actions/back.png"));
        goBackButton.setToolTipText("Go back");
        goBackButton.addActionListener(e -> goBack());
        goBackButton.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        actionToolbar.add(goBackButton);

        JButton goForwardButton = new JButton(UIUtils.getIconFromResources("actions/next.png"));
        goForwardButton.setToolTipText("Go forward");
        goForwardButton.addActionListener(e -> goForward());
        goForwardButton.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        actionToolbar.add(goForwardButton);

        JButton goUpButton = new JButton(UIUtils.getIconFromResources("actions/go-parent-folder.png"));
        goUpButton.setToolTipText("Go to parent directory");
        goUpButton.addActionListener(e -> goToParentDirectory());
        goUpButton.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        actionToolbar.add(goUpButton);

        actionToolbar.addSeparator();
        bookmarkToggle = new JToggleButton("Bookmark", UIUtils.getIconFromResources("actions/bookmark.png"));
        bookmarkToggle.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        bookmarkToggle.addActionListener(e -> {
            toggleBookmark(fileChooserComponent.getCurrentDirectory(), bookmarkToggle.isSelected());
        });
        actionToolbar.add(bookmarkToggle);

        actionToolbar.add(Box.createHorizontalGlue());

        // View button group
        FilePane filePane = ((FileChooserComponentUI)fileChooserComponent.getUI()).getFilePane();
        ButtonGroup viewButtonGroup = new ButtonGroup();

        // List Button
        JToggleButton listViewButton = new JToggleButton(UIUtils.getIconFromResources("actions/view-list.png"));
        listViewButton.setToolTipText("Display as list of items");
        listViewButton.setSelected(true);
        UIUtils.makeFlat25x25(listViewButton);
        listViewButton.addActionListener(filePane.getViewTypeAction(FilePane.VIEWTYPE_LIST));
        actionToolbar.add(listViewButton);
        viewButtonGroup.add(listViewButton);

        // Details Button
        JToggleButton detailsViewButton = new JToggleButton(UIUtils.getIconFromResources("actions/view-list-details.png"));
        detailsViewButton.setToolTipText("Display as detailed list of items");
        UIUtils.makeFlat25x25(detailsViewButton);
        detailsViewButton.addActionListener(filePane.getViewTypeAction(FilePane.VIEWTYPE_DETAILS));
        actionToolbar.add(detailsViewButton);
        viewButtonGroup.add(detailsViewButton);

        filePane.addPropertyChangeListener(e -> {
            if ("viewType".equals(e.getPropertyName())) {
                int viewType = filePane.getViewType();
                switch (viewType) {
                    case FilePane.VIEWTYPE_LIST:
                        listViewButton.setSelected(true);
                        break;

                    case FilePane.VIEWTYPE_DETAILS:
                        detailsViewButton.setSelected(true);
                        break;
                }
            }
        });
    }

    private void toggleBookmark(File directory, boolean bookmarked) {
        if(JIPipe.isInstantiated() && !JIPipe.getInstance().isInitializing()) {
            FileChooserSettings settings = FileChooserSettings.getInstance();
            Path path = directory.toPath();
            if(bookmarked) {
                if(!settings.getBookmarks().contains(path)) {
                    settings.getBookmarks().add(path);
                    settings.triggerParameterChange("bookmarks");
                }
            }
            else {
                settings.getBookmarks().remove(path);
                settings.triggerParameterChange("bookmarks");
            }
            updateLinks();
        }
    }

    public void setInitialDirectory(Path directory) {
        directory = directory.toAbsolutePath();
        initialDirectory = directory;
        fileChooserComponent.setCurrentDirectory(directory.toFile());
        history.clear();
    }

    public Path getInitialDirectory() {
        return initialDirectory;
    }

    private void goToParentDirectory() {
        fileChooserComponent.changeToParentDirectory();
    }

    private void goForward() {
        if(!history.isEmpty()) {
            if(currentHistoryIndex + 1 >= 0 && currentHistoryIndex + 1 < history.size()) {
                List<Path> backup = new ArrayList<>(history);
                int indexBackup = currentHistoryIndex;
                Path target = history.get(currentHistoryIndex + 1);
                fileChooserComponent.setCurrentDirectory(target.toFile());
                this.history = backup;
                this.currentHistoryIndex = indexBackup + 1;
            }
        }
    }

    private void goBack() {
        if(!history.isEmpty()) {
            if(currentHistoryIndex - 1 >= 0) {
                List<Path> backup = new ArrayList<>(history);
                int indexBackup = currentHistoryIndex;
                Path target = history.get(currentHistoryIndex - 1);
                fileChooserComponent.setCurrentDirectory(target.toFile());
                this.history = backup;
                this.currentHistoryIndex = indexBackup - 1;
            }
            else {
                List<Path> backup = new ArrayList<>(history);
                int indexBackup = currentHistoryIndex;
                fileChooserComponent.setCurrentDirectory(initialDirectory.toFile());
                this.history = backup;
                this.currentHistoryIndex = indexBackup - 1;
            }
        }
    }

    private void addToHistory(Path directory) {
        if(!history.isEmpty()) {
            if(currentHistoryIndex != history.size() - 1) {
                for (int i = currentHistoryIndex; i < history.size(); i++) {
                    if(!history.isEmpty())
                        history.remove(history.size() - 1);
                }
            }
        }
        history.add(directory);
        currentHistoryIndex = history.size() - 1;
    }

    private void updateLinks() {
        linkPanel.clear();

        // Standard places
        addLinkCategory("Places");
        if(!SystemUtils.IS_OS_WINDOWS) {
            addLink("Home", UIUtils.getIconFromResources("places/user-home.png"), fileSystemView.getHomeDirectory());
        }
        else {
            addLink("Home", UIUtils.getIconFromResources("places/user-home.png"),
                    fileSystemView.getHomeDirectory().toPath().getParent().toFile());
        }
        boolean useShellFolder = FilePane.usesShellFolder(fileChooserComponent);

        File[] baseFolders = (useShellFolder)
                ? (File[]) ShellFolder.get("fileChooserComboBoxFolders")
                : fileSystemView.getRoots();

        for (File root : baseFolders) {
            if(!SystemUtils.IS_OS_WINDOWS) {
                String name;
                if(root.toString().equals("/"))
                    name = "Root";
                else
                    name = root.getName();
                addLink(name, UIUtils.getIconFromResources("places/folder-root.png"), root);
            }
            else {
                Icon icon;
                if (fileSystemView.isFloppyDrive(root)) {
                    icon = UIUtils.getIconFromResources("devices/media-floppy.png");
                } else if (fileSystemView.isDrive(root)) {
                    icon = UIUtils.getIconFromResources("devices/drive-harddisk.png");
                } else if (fileSystemView.isComputerNode(root)) {
                    continue;
                } else {
                    continue;
                }
                if(root.toPath().getParent() != null)
                    continue;
                addLink(root.toString(), icon, root);
            }
        }
        addLink("ImageJ", UIUtils.getIconFromResources("apps/imagej.png"),
                Paths.get(Prefs.getImageJDir() != null ? Prefs.getImageJDir() : "").toAbsolutePath().toFile());

        if(JIPipe.isInstantiated() && !JIPipe.getInstance().isInitializing()) {
            FileChooserSettings settings = FileChooserSettings.getInstance();

            // Last directories
            Set<Path> lastDirectories = new HashSet<>();
            Path defaultDir = Paths.get("").toAbsolutePath();
            for (FileChooserSettings.LastDirectoryKey key : FileChooserSettings.LastDirectoryKey.values()) {
                Path path = settings.getLastDirectoryBy(key);
                if(!Objects.equals(path, defaultDir) && Files.isDirectory(path)) {
                    lastDirectories.add(path);
                }
            }
            for (Path path : lastDirectories) {
                String name = renderPathLabel(path);
                addLink(name,
                        UIUtils.getIconFromResources("places/folder-recent.png"),
                        path.toFile());
            }

            // Bookmarks
            List<Path> bookmarks = settings.getBookmarks().stream().filter(Files::isDirectory).collect(Collectors.toList());
            if(!bookmarks.isEmpty()) {
                addLinkCategory("Bookmarks");
                for (Path path : bookmarks) {
                    String name = renderPathLabel(path);
                    addLink(name,
                            UIUtils.getIconFromResources("actions/bookmark.png"),
                            path.toFile());
                }
            }
        }

        linkPanel.addVerticalGlue();
    }

    private String renderPathLabel(Path path) {
        String parent = "";
        if(path.getParent() != null && !Objects.equals(path.getParent(), path)) {
            parent = path.getParent().toAbsolutePath().toString();
        }
        if(parent.length() > 40) {
            parent = parent.substring(parent.length() - 40) + File.separator + "...";
        }
        if(!parent.isEmpty() && !parent.endsWith(File.separator)) {
            parent += File.separator;
        }
        return parent + StringUtils.orElse(path.getFileName(), path.toString());
    }

    public void addLinkCategory(String label) {
        linkPanel.addGroupHeader(label, null);
    }

    public void addLink(String label, Icon icon, File target) {
        JButton button = new JButton(label, icon);
        button.setToolTipText(target.toString());
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(e -> {
            fileChooserComponent.setCurrentDirectory(target);
        });
        button.setIconTextGap(12);
        button.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        linkPanel.addWideToForm(button, null);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
            addToHistory(fileChooserComponent.getCurrentDirectory().toPath());
            updateDrillDown();
            updateBookmarkToggle();
        }
    }

    private void updateBookmarkToggle() {
        if(JIPipe.isInstantiated() && !JIPipe.getInstance().isInitializing()) {
            FileChooserSettings settings = FileChooserSettings.getInstance();
            if(fileChooserComponent.getCurrentDirectory() != null) {
                bookmarkToggle.setSelected(settings.getBookmarks().contains(fileChooserComponent.getCurrentDirectory().toPath()));
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
            returnValue = JFileChooser.APPROVE_OPTION;
        }
        else if(JFileChooser.CANCEL_SELECTION.equals(e.getActionCommand())) {
            returnValue = JFileChooser.CANCEL_OPTION;
        }
        if(dialog != null) {
            dialog.setVisible(false);
        }
    }

    protected JDialog createDialog(Component parent) throws HeadlessException {
        FileChooserUI ui = fileChooserComponent.getUI();
        String title = ui.getDialogTitle(fileChooserComponent);
        putClientProperty(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY,
                title);

        JDialog dialog;
        Window window;
        if(parent instanceof Window)
            window = (Window) parent;
        else
            window = SwingUtilities.getWindowAncestor(parent);
        if (window instanceof Frame) {
            dialog = new JDialog((Frame)window, title, true);
        } else {
            dialog = new JDialog((Dialog)window, title, true);
        }
        dialog.setComponentOrientation(this.getComponentOrientation());

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(this, BorderLayout.CENTER);

        if (JDialog.isDefaultLookAndFeelDecorated()) {
            boolean supportsWindowDecorations =
                    UIManager.getLookAndFeel().getSupportsWindowDecorations();
            if (supportsWindowDecorations) {
                dialog.getRootPane().setWindowDecorationStyle(JRootPane.FILE_CHOOSER_DIALOG);
            }
        }
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(parent);

        return dialog;
    }

    public int showDialog(Component parent, String approveButtonText)
            throws HeadlessException {
        if (dialog != null) {
            // Prevent to show second instance of dialog if the previous one still exists
            return JFileChooser.ERROR_OPTION;
        }

        if(approveButtonText != null) {
            fileChooserComponent.setApproveButtonText(approveButtonText);
            setDialogType(JFileChooser.CUSTOM_DIALOG);
        }
        dialog = createDialog(parent);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                returnValue = JFileChooser.CANCEL_OPTION;
            }
        });
        returnValue = JFileChooser.ERROR_OPTION;
        fileChooserComponent.rescanCurrentDirectory();

        dialog.setVisible(true);
        firePropertyChange("JFileChooserDialogIsClosingProperty", dialog, null);

        // Remove all components from dialog. The MetalFileChooserUI.installUI() method (and other LAFs)
        // registers AWT listener for dialogs and produces memory leaks. It happens when
        // installUI invoked after the showDialog method.
        dialog.getContentPane().removeAll();
        dialog.dispose();
        dialog = null;
        return returnValue;
    }

    public int getDialogType() {
        return dialogType;
    }

    public void setDialogType(int dialogType) {
        this.dialogType = dialogType;
    }

    public int showSaveDialog(Component parent) throws HeadlessException {
        setDialogType(JFileChooser.SAVE_DIALOG);
        return showDialog(parent, null);
    }

    public int showOpenDialog(Component parent) throws HeadlessException {
        setDialogType(JFileChooser.OPEN_DIALOG);
        return showDialog(parent, null);
    }

    public void setDialogTitle(String title) {
        fileChooserComponent.setDialogTitle(title);
    }

    public void setFileSelectionMode(int mode) {
        fileChooserComponent.setFileSelectionMode(mode);
    }

    public void addChoosableFileFilter(FileNameExtensionFilter extensionFilter) {
        fileChooserComponent.addChoosableFileFilter(extensionFilter);
    }

    public void setFileFilter(FileNameExtensionFilter extensionFilter) {
        fileChooserComponent.setFileFilter(extensionFilter);
    }

    public File getSelectedFile() {
        return fileChooserComponent.getSelectedFile();
    }

    public FileFilter getFileFilter() {
        return fileChooserComponent.getFileFilter();
    }

    public void setMultiSelectionEnabled(boolean multiSelection) {
        fileChooserComponent.setMultiSelectionEnabled(multiSelection);
    }

    public File[] getSelectedFiles() {
        return fileChooserComponent.getSelectedFiles();
    }

    public static class FileChooserComponent extends JFileChooser {
        @Override
        public void updateUI() {
            if (isAcceptAllFileFilterUsed()) {
                removeChoosableFileFilter(getAcceptAllFileFilter());
            }
            FileChooserUI ui = new FileChooserComponentUI(this);
            setUI(ui);

            if(isAcceptAllFileFilterUsed()) {
                addChoosableFileFilter(getAcceptAllFileFilter());
            }
        }
    }

    public static class FileChooserComponentUI extends MetalFileChooserUI {

        FilePane filePane;

        public FileChooserComponentUI(JFileChooser filechooser) {
            super(filechooser);
        }

        @Override
        public void installComponents(JFileChooser fc) {
            super.installComponents(fc);

            // Remove the top panel
            for (Component component : fc.getComponents()) {
                Object constraints = ((BorderLayout) fc.getLayout()).getConstraints(component);
                if(constraints == BorderLayout.NORTH) {
                    // Remove the top pane
                    component.setVisible(false);
                }
                else if (constraints == BorderLayout.CENTER) {
                    // Add border to the list
                    filePane = (FilePane) component;
                    ((JComponent)component).setBorder(BorderFactory.createEtchedBorder());
                }
            }
        }

        public FilePane getFilePane() {
            return filePane;
        }
    }
}
