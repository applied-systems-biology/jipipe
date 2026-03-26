package org.hkijena.jipipe.plugins.expressions;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.icons.JIPipeDesktopDualIcon;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.expressions.ui.ExpressionBuilderSyntaxChecker;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.scripting.ScriptUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class DataExportExpressionParameterEditorPathChooserUI extends JDialog {

    private final JIPipeExpressionEvaluatorSyntaxTokenMaker tokenMaker = new JIPipeExpressionEvaluatorSyntaxTokenMaker();
    private final String title;
    private final JIPipeWorkbench workbench;
    private final PathType pathType;
    private final FileNameExtensionFilter[] extensions;
    private DataExportExpressionParameter outputParameter;

    private List<Entry> directoryEntryList = new ArrayList<>();
    private Entry lastEntry = new Entry();

    private JPanel entryPanel = new JPanel();
    private JScrollPane entryPanelScrollPane;
    private RSyntaxTextArea expressionEditor;

    public DataExportExpressionParameterEditorPathChooserUI(Window owner, String title, JIPipeWorkbench workbench, PathType pathType, FileNameExtensionFilter[] extensions) {
        super(owner);
        this.title = title;
        this.workbench = workbench;
        this.pathType = pathType;
        this.extensions = extensions;
        initialize();
        initializeEntries();
        onEntriesUpdated();
    }

    private void initialize() {
         setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        setTitle(title);
        setModal(true);
        setIconImage(UIUtils.getJIPipeIcon128());
        JPanel contentPane = new JPanel();
        contentPane.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        contentPane.setLayout(new BorderLayout(8, 8));
        setContentPane(contentPane);

        contentPane.add(UIUtils.createInfoLabel("Exporter path builder",
                "This tool helps you with building a portable (i.e. can be transferred to other PCs) path for your exporter nodes. " +
                        "Click the items to build the output folder and subfolders, as well as the file name. " +
                        "You can also simulate saving a file and let the tool guess most of the settings for you."), BorderLayout.NORTH);

        // Init entry panel
        entryPanel.setLayout(new BoxLayout(entryPanel, BoxLayout.X_AXIS));
        this.entryPanelScrollPane = new JScrollPane(entryPanel);

        // Init expression preview
        TokenMakerFactory tokenMakerFactory = new TokenMakerFactory() {
            @Override
            protected TokenMaker getTokenMakerImpl(String key) {
                return tokenMaker;
            }

            @Override
            public Set<String> keySet() {
                return Collections.singleton("text/expression");
            }
        };
        RSyntaxDocument document = new RSyntaxDocument(tokenMakerFactory, "text/expression");
        expressionEditor = new RSyntaxTextArea(document);
        ThemeUtils.applyThemeToCodeEditor(expressionEditor);
        expressionEditor.getCaret().setVisible(true);
        expressionEditor.setBackground(UIManager.getColor("TextArea.background"));
        expressionEditor.setLineWrap(true);
        expressionEditor.setHighlightCurrentLine(false);
        expressionEditor.setEditable(false);

        // Wrapper panels
        JPanel entryWrapperPanel = new JPanel(new BorderLayout(8,8));
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(UIUtils.createButton("Auto-guess from path", "Simulate a file saving procedure and automatically design appropriate settings", JIPipe.RESOURCES.getIcon16("actions/wand-magic-sparkles.png"), this::tryAutoPopulateFromPath));
        entryWrapperPanel.add(toolBar, BorderLayout.NORTH);
        entryWrapperPanel.add(entryPanelScrollPane, BorderLayout.CENTER);
        entryWrapperPanel.add(UIUtils.createJLabel("Click the path items to change their contents", JIPipe.RESOURCES.getIcon16("actions/input-mouse-click-left.png")), BorderLayout.SOUTH);

        JPanel previewWrapperPanel = new JPanel(new BorderLayout(8,8));
        previewWrapperPanel.add(UIUtils.createJLabel("Preview", JIPipe.RESOURCES.getIcon16("actions/preview.png")), BorderLayout.NORTH);
        previewWrapperPanel.add(new JScrollPane(expressionEditor), BorderLayout.CENTER);
        previewWrapperPanel.add(new ExpressionBuilderSyntaxChecker(expressionEditor), BorderLayout.SOUTH);

        // Init split pane
        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.TOP_BOTTOM,
                UIUtils.wrapInIslandPanelIfNeeded(entryWrapperPanel),
                UIUtils.wrapInIslandPanelIfNeeded(previewWrapperPanel), 0.5);
        splitPane.setBorder(UIUtils.createEmptyBorder(8));
        splitPane.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());


        contentPane.add(splitPane, BorderLayout.CENTER);

        initializeButtonPanel();
    }

    private void tryAutoPopulateFromPath() {
        Path path = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Simulate saving", HTMLText.EMPTY, extensions);
        if(path != null) {
            path = path.toAbsolutePath();
            
            JIPipeProject project = workbench.getProject();
            List<Entry> newDirectoryEntries = new ArrayList<>();
            Entry newLastEntry = new Entry();
            
            // Try to match against project directory or user paths
            boolean matched = false;
            
            if(project != null) {
                Path projectPath = project.getWorkDirectory();
                
                // Check if path is within the project directory
                if(projectPath != null && path.startsWith(projectPath)) {
                    matched = true;
                    Entry rootEntry = new Entry();
                    rootEntry.sourceType = EntrySourceType.ProjectDirectory;
                    newDirectoryEntries.add(rootEntry);
                    
                    // Add relative path components
                    Path relativePath = projectPath.relativize(path);
                    addRelativePathEntries(relativePath, newDirectoryEntries, newLastEntry);
                }
                
                // Check if path is within a project user path
                if(!matched && projectPath != null) {
                    for(Map.Entry<String, Path> userPathEntry : project.getMetadata().getUserPaths().getDirectoryMap(projectPath).entrySet()) {
                        Path userPath = userPathEntry.getValue();
                        if(path.startsWith(userPath)) {
                            matched = true;
                            
                            Entry rootEntry = new Entry();
                            rootEntry.sourceType = EntrySourceType.ProjectUserPath;
                            rootEntry.content = userPathEntry.getKey();
                            newDirectoryEntries.add(rootEntry);
                            
                            // Add relative path components
                            Path relativePath = userPath.relativize(path);
                            addRelativePathEntries(relativePath, newDirectoryEntries, newLastEntry);
                            break;
                        }
                    }
                }
            }
            
            // If no match found, use custom entries for the full path
            if(!matched) {
                Path parent = path.getParent();
                String fileName = path.getFileName() != null ? path.getFileName().toString() : "";
                
                if(parent != null) {
                    for(Path component : parent) {
                        Entry entry = new Entry();
                        entry.sourceType = EntrySourceType.Custom;
                        entry.content = component.toString();
                        newDirectoryEntries.add(entry);
                    }
                }
                
                newLastEntry.sourceType = EntrySourceType.Custom;
                newLastEntry.content = fileName;
            }
            
            // Ensure at least one directory entry exists
            if(newDirectoryEntries.isEmpty()) {
                Entry defaultEntry = new Entry();
                defaultEntry.sourceType = EntrySourceType.ProjectDirectory;
                newDirectoryEntries.add(defaultEntry);
            }
            
            // Apply the new configuration
            directoryEntryList = newDirectoryEntries;
            lastEntry = newLastEntry;
            onEntriesUpdated();
        }
    }
    
    /**
     * Adds entries from a relative path, splitting directories and the final filename.
     *
     * @param relativePath the relative path to process
     * @param directoryEntries list to add directory entries to
     * @param lastEntry the last entry (filename) to populate
     */
    private void addRelativePathEntries(Path relativePath, List<Entry> directoryEntries, Entry lastEntry) {
        // Convert path to use forward slashes for consistency
        String pathStr = relativePath.toString().replace('\\', '/');
        
        // Split into components
        String[] components = pathStr.split("/");
        
        // All but the last component are directories
        for(int i = 0; i < components.length - 1; i++) {
            if(!components[i].isEmpty()) {
                Entry entry = new Entry();
                entry.sourceType = EntrySourceType.Custom;
                entry.content = components[i];
                directoryEntries.add(entry);
            }
        }
        
        // The last component is the filename
        if(components.length > 0 && !components[components.length - 1].isEmpty()) {
            lastEntry.sourceType = EntrySourceType.Custom;
            lastEntry.content = components[components.length - 1];
        } else {
            // Default to auto if no filename
            lastEntry.sourceType = EntrySourceType.Auto;
        }
    }

    private void initializeEntries() {
        directoryEntryList.clear();

        // The first directory entry
        Entry entry = new Entry();
        entry.sourceType = EntrySourceType.ProjectDirectory;
        directoryEntryList.add(entry);

        // Pre-init the last entry
        lastEntry.sourceType = EntrySourceType.Auto;
    }

    private String buildExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("PATH_COMBINE(");

        // Resolve first entry
        Entry firstEntry = directoryEntryList.getFirst();
        switch (firstEntry.sourceType) {
            case ProjectDirectory: {
                sb.append("project_dir");
            }
            break;
            case Auto: {
                sb.append("data_dir");
            }
            break;
            case ProjectUserPath: {
                if(ScriptUtils.isValidVariableName(firstEntry.content)) {
                    sb.append("project_user_path.").append(firstEntry.content);
                }
                else {
                    sb.append("project_user_paths @ \"").append(ScriptUtils.escapeString(firstEntry.content)).append("\"");
                }
            }
            break;
            case Variable: {
                sb.append(JIPipeExpressionEvaluator.escapeVariable(firstEntry.content));
            }
            break;
            case JoinedVariables: {
                if (firstEntry.joinedVariables != null) {
                    sb.append(firstEntry.joinedVariables.toExpression());
                } else {
                    return "INVALID_EXPRESSION";
                }
            }
            break;
            case Expression: {
                sb.append(firstEntry.content);
            }
            break;
            case Custom: {
                sb.append("\"").append(ScriptUtils.escapeString(firstEntry.content)).append("\"");
            }
            break;
            default: return "INVALID_EXPRESSION";
        }

        // Resolve intermediate entries
        for (int i = 1; i < directoryEntryList.size(); i++) {
            Entry entry = directoryEntryList.get(i);

            sb.append(", ");

            // Resolve entry
            switch (entry.sourceType) {
                case Variable: {
                    sb.append(JIPipeExpressionEvaluator.escapeVariable(entry.content));
                }
                break;
                case JoinedVariables: {
                    if (entry.joinedVariables != null) {
                        sb.append(entry.joinedVariables.toExpression());
                    } else {
                        return "INVALID_EXPRESSION";
                    }
                }
                break;
                case Expression: {
                    sb.append(entry.content);
                }
                break;
                case Custom: {
                    sb.append("\"").append(ScriptUtils.escapeString(entry.content)).append("\"");
                }
                break;
                default: return "INVALID_EXPRESSION";
            }
        }

        sb.append(", ");

        // Resolve last entry
        switch (lastEntry.sourceType) {
            case Auto: {
                sb.append("auto_file_name");
            }
            break;
            case Variable: {
                if(ScriptUtils.isValidVariableName(lastEntry.content)) {
                    sb.append(lastEntry.content);
                }
                else {
                    sb.append("$\"").append(ScriptUtils.escapeString(lastEntry.content)).append("\"");
                }
            }
            break;
            case JoinedVariables: {
                if (lastEntry.joinedVariables != null) {
                    sb.append(lastEntry.joinedVariables.toExpression());
                } else {
                    return "INVALID_EXPRESSION";
                }
            }
            break;
            case Expression: {
                sb.append(lastEntry.content);
            }
            break;
            case Custom: {
                sb.append("\"").append(ScriptUtils.escapeString(lastEntry.content)).append("\"");
            }
            break;
            default: return "INVALID_EXPRESSION";
        }

        sb.append(")");
        return sb.toString();
    }

    private void onEntriesUpdated() {
        expressionEditor.setText(buildExpression());
        rebuildEntryUI();
    }

    private void rebuildEntryUI() {
        int lastHorizontalValue = entryPanelScrollPane.getHorizontalScrollBar().getValue();
        entryPanel.removeAll();

        for (int i = 0; i < directoryEntryList.size(); i++) {
            Entry e = directoryEntryList.get(i);
            addDirectoryEntryButton(entryPanel, e, i);
            addInsertDirectoryEntryButton(entryPanel, i);
            entryPanel.add(new JLabel(JIPipe.RESOURCES.getIcon24("actions/arrow-right.png")));
        }

        addLastEntryButton(entryPanel, lastEntry);

        revalidate();
        repaint(50);

        SwingUtilities.invokeLater(() -> {
            entryPanelScrollPane.getHorizontalScrollBar().setValue(lastHorizontalValue);
        });
    }

    private void addInsertDirectoryEntryButton(JPanel entryPanel, int insertAfter) {
        JButton button = new JButton(JIPipe.RESOURCES.getIcon16("actions/plus.png"));
        button.setToolTipText("Add subdirectory here");
        button.addActionListener(e -> {
            insertSubDirectoryEntryAfter(insertAfter);
        });
        entryPanel.add(button);
    }

    private void insertSubDirectoryEntryAfter(int insertAfter) {
        Entry entry = new Entry();
        entry.content = "Unnamed";
        entry.sourceType = EntrySourceType.Custom;
        directoryEntryList.add(insertAfter + 1, entry);
        onEntriesUpdated();
    }

    private void addDirectoryEntryButton(JPanel entryPanel, Entry entry, int i) {
        JButton button = new JButton();
        var sourceIcon = switch (entry.sourceType) {
            case Auto -> JIPipe.RESOURCES.getIcon16("actions/wand-magic-sparkles.png");
            case Expression -> JIPipe.RESOURCES.getIcon16("actions/insert-math-expression.png");
            case Custom -> JIPipe.RESOURCES.getIcon16("actions/text-convert-to-regular.png");
            case Variable -> JIPipe.RESOURCES.getIcon16("data-types/annotation.png");
            case JoinedVariables -> JIPipe.RESOURCES.getIcon16("data-types/annotation.png");
            case ProjectUserPath -> JIPipe.RESOURCES.getIcon16("actions/preferences-system-symbolic.png");
            case ProjectDirectory -> JIPipe.RESOURCES.getIcon16("jipipe.png");
            default -> JIPipe.RESOURCES.getIcon16("missing.png");
        };
        if (entry.sourceType == EntrySourceType.Auto) {
            button.setText("Automatically generated");
            button.setFont(button.getFont().deriveFont(Font.ITALIC));
        } else if (entry.sourceType == EntrySourceType.ProjectDirectory) {
            button.setText("Project directory");
        } else if (entry.sourceType == EntrySourceType.JoinedVariables) {
            if (entry.joinedVariables != null) {
                button.setText(entry.joinedVariables.toString());
                button.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            } else {
                button.setText("(empty joined variables)");
            }
        } else if (entry.sourceType == EntrySourceType.Expression || entry.sourceType == EntrySourceType.Variable || entry.sourceType == EntrySourceType.ProjectUserPath) {
            button.setText(StringUtils.nullToEmpty(entry.content));
            button.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        } else {
            button.setText(StringUtils.nullToEmpty(entry.content));
        }
        button.setIcon(new JIPipeDesktopDualIcon(JIPipe.RESOURCES.getIcon16("data-types/folder.png"), sourceIcon, 4));
        entryPanel.add(button);

        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(button);
        if (i == 0) {
            popupMenu.add(UIUtils.createMenuItem("Automatically generated",
                    "JIPipe will create a temporary directory",
                    JIPipe.RESOURCES.getIcon16("actions/wand-magic-sparkles.png"),
                    () -> {
                        entry.sourceType = EntrySourceType.Auto;
                        onEntriesUpdated();
                    }));
            popupMenu.add(UIUtils.createMenuItem("Project directory",
                    "Use the project directory as root",
                    JIPipe.RESOURCES.getIcon16("jipipe.png"),
                    () -> {
                        entry.sourceType = EntrySourceType.ProjectDirectory;
                        onEntriesUpdated();
                    }));
            popupMenu.add(UIUtils.createMenuItem("Project user path",
                    "Use a project-wide user path",
                    JIPipe.RESOURCES.getIcon16("actions/preferences-system-symbolic.png"),
                    () -> {
                        JIPipeProject project = workbench.getProject();
                        if(project != null) {
                            Map<String, Path> userPathMap = project.getUserPathMap();

                            if(userPathMap.isEmpty()) {
                                JOptionPane.showMessageDialog(this, "No project directories configured. Please visit the project settings.", "Select project user path", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            Object selected = JOptionPane.showInputDialog(this,
                                    "Select the project user path key",
                                    "Use project-wide user path",
                                    JOptionPane.PLAIN_MESSAGE,
                                    null,
                                    userPathMap.keySet().toArray(new String[0]),
                                    userPathMap.keySet().iterator().next());
                            if(selected instanceof String) {
                                entry.sourceType = EntrySourceType.ProjectUserPath;
                                entry.content = (String) selected;
                                onEntriesUpdated();
                            }
                        }
                    }));
            popupMenu.add(UIUtils.createMenuItem("Custom",
                    "Select a custom root directory",
                    JIPipe.RESOURCES.getIcon16("actions/folder-open.png"),
                    () -> {
                        Path directory = JIPipeDesktop.openDirectory(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Select root directory", HTMLText.EMPTY);
                        if (directory != null) {
                            entry.sourceType = EntrySourceType.Custom;
                            entry.content = directory.toString();
                            onEntriesUpdated();
                        }
                    }));
        }
        popupMenu.add(UIUtils.createMenuItem("Annotation/Variable",
                "Use an annotation or expression variable",
                JIPipe.RESOURCES.getIcon16("data-types/annotation.png"),
                () -> {
                    String name = JOptionPane.showInputDialog(this, "Enter variable name", "");
                    if (!StringUtils.isNullOrEmpty(name)) {
                        entry.sourceType = EntrySourceType.Variable;
                        entry.content = name;
                        onEntriesUpdated();
                    }
                }));
        popupMenu.add(UIUtils.createMenuItem("Joined annotations/variables",
                "Join multiple annotations/variables with a custom delimiter",
                JIPipe.RESOURCES.getIcon16("data-types/annotation.png"),
                () -> {
                    JoinedVariablesEntry newEntry = new JoinedVariablesEntry();
                    boolean confirmed = JIPipeDesktopParameterFormPanel.showDialog(
                            (JIPipeDesktopWorkbench) workbench,
                            DataExportExpressionParameterEditorPathChooserUI.this,
                            newEntry,
                            MarkdownText.EMPTY,
                            "Configure joined variables",
                            JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
                    if (confirmed) {
                        entry.sourceType = EntrySourceType.JoinedVariables;
                        entry.joinedVariables = newEntry;
                        onEntriesUpdated();
                    }
                }));
        popupMenu.add(UIUtils.createMenuItem("Expression",
                "Input a raw expression",
                JIPipe.RESOURCES.getIcon16("actions/insert-math-expression.png"),
                () -> {
                    String name = JOptionPane.showInputDialog(this, "Enter expression", "");
                    if (!StringUtils.isNullOrEmpty(name)) {
                        entry.sourceType = EntrySourceType.Expression;
                        entry.content = name;
                        onEntriesUpdated();
                    }
                }));
        if (i > 0) {
            popupMenu.add(UIUtils.createMenuItem("Custom",
                    "Enters a custom name",
                    JIPipe.RESOURCES.getIcon16("actions/text-convert-to-regular.png"),
                    () -> {
                        String name = JOptionPane.showInputDialog(this, "Enter custom name", "");
                        if (!StringUtils.isNullOrEmpty(name)) {
                            entry.sourceType = EntrySourceType.Custom;
                            entry.content = name;
                            onEntriesUpdated();
                        }
                    }));
            popupMenu.addSeparator();
            popupMenu.add(UIUtils.createMenuItem("Remove", "Removes this subdirectory",
                    JIPipe.RESOURCES.getIcon16("actions/edit-delete.png"), () -> {
                        deleteDirectoryEntryAt(i);
                    }));
        }
    }

    private void deleteDirectoryEntryAt(int i) {
        if (i > 0 && i < directoryEntryList.size()) {
            directoryEntryList.remove(i);
            onEntriesUpdated();
        }
    }

    private void addLastEntryButton(JPanel entryPanel, Entry entry) {
        JButton button = new JButton();
        button.setBorder(UIUtils.createSuccessBorder());
        var typeIcon = switch (pathType) {
            case FilesOnly -> JIPipe.RESOURCES.getIcon16("data-types/file.png");
            case DirectoriesOnly -> JIPipe.RESOURCES.getIcon16("data-types/folder.png");
            default -> JIPipe.RESOURCES.getIcon16("data-types/path.png");
        };
        var sourceIcon = switch (entry.sourceType) {
            case Auto -> JIPipe.RESOURCES.getIcon16("actions/wand-magic-sparkles.png");
            case Expression -> JIPipe.RESOURCES.getIcon16("actions/insert-math-expression.png");
            case Custom -> JIPipe.RESOURCES.getIcon16("actions/text-convert-to-regular.png");
            case Variable -> JIPipe.RESOURCES.getIcon16("data-types/annotation.png");
            case JoinedVariables -> JIPipe.RESOURCES.getIcon16("data-types/annotation.png");
            default -> JIPipe.RESOURCES.getIcon16("missing.png");
        };
        button.setIcon(new JIPipeDesktopDualIcon(typeIcon, sourceIcon, 4));
        if (entry.sourceType == EntrySourceType.Auto) {
            button.setText("Automatically generated");
            button.setFont(button.getFont().deriveFont(Font.ITALIC));
        } else if (entry.sourceType == EntrySourceType.JoinedVariables) {
            if (entry.joinedVariables != null) {
                button.setText(entry.joinedVariables.toString());
                button.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            } else {
                button.setText("(empty joined variables)");
            }
        } else if (entry.sourceType == EntrySourceType.Expression || entry.sourceType == EntrySourceType.Variable) {
            button.setText(StringUtils.nullToEmpty(entry.content));
            button.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        } else {
            button.setText(StringUtils.nullToEmpty(entry.content));
        }

        entryPanel.add(button);

        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(button);
        popupMenu.add(UIUtils.createMenuItem("Automatically generated",
                "JIPipe will create a temporary directory",
                JIPipe.RESOURCES.getIcon16("actions/wand-magic-sparkles.png"),
                () -> {
                    entry.sourceType = EntrySourceType.Auto;
                    onEntriesUpdated();
                }));
        popupMenu.add(UIUtils.createMenuItem("Annotation/Variable",
                "Use an annotation or expression variable",
                JIPipe.RESOURCES.getIcon16("data-types/annotation.png"),
                () -> {
                    String name = JOptionPane.showInputDialog(this, "Enter variable name", "");
                    if (!StringUtils.isNullOrEmpty(name)) {
                        entry.sourceType = EntrySourceType.Variable;
                        entry.content = name;
                        onEntriesUpdated();
                    }
                }));
        popupMenu.add(UIUtils.createMenuItem("Joined annotations/variables",
                "Join multiple annotations/variables with a custom delimiter",
                JIPipe.RESOURCES.getIcon16("data-types/annotation.png"),
                () -> {
                    JoinedVariablesEntry newEntry = new JoinedVariablesEntry();
                    boolean confirmed = JIPipeDesktopParameterFormPanel.showDialog(
                            (JIPipeDesktopWorkbench) workbench,
                            DataExportExpressionParameterEditorPathChooserUI.this,
                            newEntry,
                            MarkdownText.EMPTY,
                            "Configure joined variables",
                            JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS);
                    if (confirmed) {
                        entry.sourceType = EntrySourceType.JoinedVariables;
                        entry.joinedVariables = newEntry;
                        onEntriesUpdated();
                    }
                }));
        popupMenu.add(UIUtils.createMenuItem("Expression",
                "Input a raw expression",
                JIPipe.RESOURCES.getIcon16("actions/insert-math-expression.png"),
                () -> {
                    String name = JOptionPane.showInputDialog(this, "Enter expression", "");
                    if (!StringUtils.isNullOrEmpty(name)) {
                        entry.sourceType = EntrySourceType.Expression;
                        entry.content = name;
                        onEntriesUpdated();
                    }
                }));
        popupMenu.add(UIUtils.createMenuItem("Custom",
                "Enters a custom name",
                JIPipe.RESOURCES.getIcon16("actions/text-convert-to-regular.png"),
                () -> {
                    String name = JOptionPane.showInputDialog(this, "Enter custom name", "");
                    if (!StringUtils.isNullOrEmpty(name)) {
                        entry.sourceType = EntrySourceType.Custom;
                        entry.content = name;
                        onEntriesUpdated();
                    }
                }));
    }


    public DataExportExpressionParameter getOutputParameter() {
        return outputParameter;
    }

    private boolean tryGenerateOutputParameter() {
        String s = buildExpression();
        if(!StringUtils.isNullOrEmpty(s) && !"INVALID_EXPRESSION".equals(s)) {
            outputParameter = new DataExportExpressionParameter(s);
            return true;
        }
        return false;
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("OK", JIPipe.RESOURCES.getIcon16("actions/dialog-ok.png"));
        confirmButton.addActionListener(e -> {
            if (tryGenerateOutputParameter()) {
                setVisible(false);
            }
        });
        buttonPanel.add(confirmButton);
        UIUtils.makeNonOpaque(buttonPanel, true);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    public void tryImportCurrentExpression(String currentExpression) {
        if (StringUtils.isNullOrEmpty(currentExpression)) {
            return;
        }

        currentExpression = currentExpression.trim();

        try {
            // Check if it's a simple string literal "<path>"
            if (currentExpression.startsWith("\"") && currentExpression.endsWith("\"")) {
                tryImportSimpleStringLiteral(currentExpression);
                return;
            }

            // Check if it's PATH_COMBINE(...)
            if (currentExpression.startsWith("PATH_COMBINE(") && currentExpression.endsWith(")")) {
                tryImportPathCombineExpression(currentExpression);
                return;
            }

            // If neither, ignore the expression (invalid format)
        } catch (Exception e) {
            // If parsing fails, leave the current state unchanged
        }
    }

    private void tryImportSimpleStringLiteral(String expression) {
        // Extract the string content from "<path>"
        String path = JIPipeExpressionEvaluator.unescapeString(expression);

        if (StringUtils.isNullOrEmpty(path)) {
            return;
        }

        // Split the path into directory entries and filename
        Path p = Path.of(path);

        List<Entry> newDirectoryEntries = new ArrayList<>();
        Entry newLastEntry = new Entry();

        // Get the parent path components
        Path parent = p.getParent();
        String fileName = p.getFileName() != null ? p.getFileName().toString() : "";

        if (parent != null) {
            // Add each parent component as a Custom entry
            for (Path component : parent) {
                Entry entry = new Entry();
                entry.sourceType = EntrySourceType.Custom;
                entry.content = component.toString();
                newDirectoryEntries.add(entry);
            }
        }

        // Set the filename as the last entry
        newLastEntry.sourceType = EntrySourceType.Custom;
        newLastEntry.content = fileName;

        // Validate: must have at least 1 directory item
        if (newDirectoryEntries.isEmpty()) {
            // Create a default directory entry
            Entry defaultEntry = new Entry();
            defaultEntry.sourceType = EntrySourceType.ProjectDirectory;
            newDirectoryEntries.add(defaultEntry);
        }

        // Apply the new configuration
        directoryEntryList = newDirectoryEntries;
        lastEntry = newLastEntry;
        onEntriesUpdated();
    }

    private void tryImportPathCombineExpression(String expression) {
        // Extract the content inside PATH_COMBINE(...)
        String innerContent = expression.substring("PATH_COMBINE(".length(), expression.length() - 1);

        // Tokenize to extract arguments
        JIPipeExpressionEvaluator evaluator = JIPipeExpressionParameter.getEvaluatorInstance();
        List<String> tokens = evaluator.tokenize(innerContent, false, true);

        // Parse arguments (split by comma, respecting parentheses and quotes)
        List<String> arguments = parseArguments(tokens);

        if (arguments.size() < 2) {
            // Invalid: need at least one directory and one file item
            return;
        }

        List<Entry> newDirectoryEntries = new ArrayList<>();
        Entry newLastEntry = new Entry();

        // Parse all but the last argument as directory entries
        for (int i = 0; i < arguments.size() - 1; i++) {
            Entry entry = parseEntry(arguments.get(i), i == 0);
            if (entry == null) {
                return; // Invalid entry, abort
            }
            newDirectoryEntries.add(entry);
        }

        // Parse the last argument as the file entry
        Entry parsedLastEntry = parseLastEntry(arguments.get(arguments.size() - 1));
        if (parsedLastEntry == null) {
            return; // Invalid entry, abort
        }
        newLastEntry = parsedLastEntry;

        // Validate: must have at least 1 directory item
        if (newDirectoryEntries.isEmpty()) {
            return;
        }

        // Apply the new configuration
        directoryEntryList = newDirectoryEntries;
        lastEntry = newLastEntry;
        onEntriesUpdated();
    }

    private List<String> parseArguments(List<String> tokens) {
        List<String> arguments = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        int parenDepth = 0;

        for (String token : tokens) {
            if ("(".equals(token)) {
                parenDepth++;
                currentArg.append(token);
            } else if (")".equals(token)) {
                parenDepth--;
                currentArg.append(token);
            } else if (",".equals(token) && parenDepth == 0) {
                String arg = currentArg.toString().trim();
                if (!arg.isEmpty()) {
                    arguments.add(arg);
                }
                currentArg = new StringBuilder();
            } else {
                currentArg.append(token);
            }
        }

        // Add the last argument
        String lastArg = currentArg.toString().trim();
        if (!lastArg.isEmpty()) {
            arguments.add(lastArg);
        }

        return arguments;
    }

    private Entry parseEntry(String argument, boolean isFirstEntry) {
        argument = argument.trim();

        if (argument.isEmpty()) {
            return null;
        }

        Entry entry = new Entry();

        // Check for project_dir (only valid for first entry)
        if ("project_dir".equals(argument)) {
            if (!isFirstEntry) {
                return null; // project_dir only valid as first entry
            }
            entry.sourceType = EntrySourceType.ProjectDirectory;
            entry.content = "";
            return entry;
        }

        // Check for data_dir (only valid for first entry, maps to Auto)
        if ("data_dir".equals(argument)) {
            if (!isFirstEntry) {
                return null; // data_dir only valid as first entry
            }
            entry.sourceType = EntrySourceType.Auto;
            entry.content = "";
            return entry;
        }

        // Check for project_user_path.key format
        if (argument.startsWith("project_user_path.")) {
            String key = argument.substring("project_user_path.".length());
            entry.sourceType = EntrySourceType.ProjectUserPath;
            entry.content = key;
            return entry;
        }

        // Check for project_user_paths @ "key" format
        if (argument.startsWith("project_user_paths @ ") || argument.startsWith("project_user_paths@")) {
            String keyPart = argument.contains(" @ ")
                ? argument.substring("project_user_paths @ ".length())
                : argument.substring("project_user_paths@".length());
            // Extract the key from quotes
            String key = extractQuotedString(keyPart);
            if (key != null) {
                entry.sourceType = EntrySourceType.ProjectUserPath;
                entry.content = key;
                return entry;
            }
        }

        // Check for string literal (Custom)
        if (argument.startsWith("\"") && argument.endsWith("\"")) {
            String content = JIPipeExpressionEvaluator.unescapeString(argument);
            entry.sourceType = EntrySourceType.Custom;
            entry.content = content;
            return entry;
        }

        // Check for variable reference with $ prefix
        if (argument.startsWith("$")) {
            String varContent = argument.substring(1);
            // Check if it's a quoted variable name
            if (varContent.startsWith("\"") && varContent.endsWith("\"")) {
                entry.sourceType = EntrySourceType.Variable;
                entry.content = JIPipeExpressionEvaluator.unescapeString(varContent);
                return entry;
            }
            // Otherwise it's a direct variable reference
            entry.sourceType = EntrySourceType.Variable;
            entry.content = varContent;
            return entry;
        }

        // Check if it's a simple variable name (valid identifier)
        if (ScriptUtils.isValidVariableName(argument)) {
            entry.sourceType = EntrySourceType.Variable;
            entry.content = argument;
            return entry;
        }

        // Otherwise, treat it as an expression
        entry.sourceType = EntrySourceType.Expression;
        entry.content = argument;
        return entry;
    }

    private Entry parseLastEntry(String argument) {
        argument = argument.trim();

        if (argument.isEmpty()) {
            return null;
        }

        Entry entry = new Entry();

        // Check for auto_file_name
        if ("auto_file_name".equals(argument)) {
            entry.sourceType = EntrySourceType.Auto;
            entry.content = "";
            return entry;
        }

        // Check for string literal (Custom)
        if (argument.startsWith("\"") && argument.endsWith("\"")) {
            String content = JIPipeExpressionEvaluator.unescapeString(argument);
            entry.sourceType = EntrySourceType.Custom;
            entry.content = content;
            return entry;
        }

        // Check for variable reference with $ prefix
        if (argument.startsWith("$")) {
            String varContent = argument.substring(1);
            // Check if it's a quoted variable name
            if (varContent.startsWith("\"") && varContent.endsWith("\"")) {
                entry.sourceType = EntrySourceType.Variable;
                entry.content = JIPipeExpressionEvaluator.unescapeString(varContent);
                return entry;
            }
            // Otherwise it's a direct variable reference
            entry.sourceType = EntrySourceType.Variable;
            entry.content = varContent;
            return entry;
        }

        // Check if it's a simple variable name (valid identifier)
        if (ScriptUtils.isValidVariableName(argument)) {
            entry.sourceType = EntrySourceType.Variable;
            entry.content = argument;
            return entry;
        }

        // Otherwise, treat it as an expression
        entry.sourceType = EntrySourceType.Expression;
        entry.content = argument;
        return entry;
    }

    private String extractQuotedString(String quotedString) {
        quotedString = quotedString.trim();
        if (quotedString.startsWith("\"") && quotedString.endsWith("\"") && quotedString.length() >= 2) {
            return JIPipeExpressionEvaluator.unescapeString(quotedString);
        }
        return null;
    }

    private static class Entry {
        private EntrySourceType sourceType;
        private String content = "";
        private JoinedVariablesEntry joinedVariables;
    }

    private enum EntrySourceType {
        ProjectDirectory,
        ProjectUserPath,
        Custom,
        Expression,
        Variable,
        JoinedVariables,
        Auto
    }
}
