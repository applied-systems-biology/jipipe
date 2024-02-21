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

package org.hkijena.jipipe.extensions.parameters.api.scripts;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.scripts.LargeScriptParameterEditorUI;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parameter editor for {@link ScriptParameter}
 */
public class ScriptParameterEditorUI extends JIPipeParameterEditorUI {

    private static final List<ExternalEditor> OPENED_EXTERNAL_EDITORS = new ArrayList<>();
    private CustomEditorPane textArea;
    private JLabel collapseInfoLabel;
    private boolean isCollapsed;
    private Component pathEditorComponent;
    private  JButton closeExternalEditorsButton;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public ScriptParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ScriptParameter code = getParameter(ScriptParameter.class);
        collapseInfoLabel = new JLabel("The code is hidden. Click the 'Collapse' button to show it",
                UIUtils.getIconFromResources("actions/eye-slash.png"),
                JLabel.LEFT);
        collapseInfoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        textArea = new CustomEditorPane();
        UIUtils.applyThemeToCodeEditor(textArea);
        textArea.setBackground(UIManager.getColor("TextArea.background"));
        textArea.setHighlightCurrentLine(false);
        if (code.getLanguage() != null) {
            textArea.setLanguage(code.getLanguage());
            // Temporarily removed for backwards compatibility
//            textArea.setAutoCompletionEnabled(true);
        }
        textArea.setTabSize(4);
        getContext().inject(textArea);
        textArea.setText(code.getCode());
        textArea.setSyntaxEditingStyle(code.getMimeType());
        textArea.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                code.setCode(textArea.getText());
                setParameter(code, false);
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(new JLabel(code.getLanguageName()));

        toolBar.add(Box.createHorizontalGlue());

        JToggleButton externalCodeToggle = new JToggleButton("Use external file", UIUtils.getIconFromResources("actions/edit-link.png"));
        externalCodeToggle.setToolTipText("If enabled, the code is extracted from an external file.");
        externalCodeToggle.addActionListener(e -> toggleExternalCode());
        toolBar.add(externalCodeToggle);

        JToggleButton collapseButton = new JToggleButton("Collapse", UIUtils.getIconFromResources("actions/eye-slash.png"));
        collapseButton.setSelected(code.isCollapsed());
        collapseButton.addActionListener(e -> toggleCollapse());
        toolBar.add(collapseButton);

        JButton openIdeButton = new JButton("Open in ...", UIUtils.getIconFromResources("actions/open-in-new-window.png"));
        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(openIdeButton);
        popupMenu.add(UIUtils.createMenuItem("New tab", "Opens the editor in a new tab", UIUtils.getIconFromResources("actions/tab-new.png"), this::openIDEInTab));
        popupMenu.add(UIUtils.createMenuItem("New window", "Opens the editor in a new window", UIUtils.getIconFromResources("actions/window_new.png"), this::openIdeInNewWindow));
        popupMenu.add(UIUtils.createMenuItem("External editor", "Opens the editor in an external application", UIUtils.getIconFromResources("actions/edit.png"), this::openExternalIde));
        toolBar.add(openIdeButton);

        add(toolBar, BorderLayout.NORTH);

        setBorder(UIUtils.createControlBorder());

        closeExternalEditorsButton = new JButton("<html><strong>External editors are currently open</strong><br>Click this button to re-enable editing",
                UIUtils.getIconFromResources("actions/unlock.png"));
        closeExternalEditorsButton.addActionListener(e -> closeExistingExternalEditors());
    }



    private void closeExistingExternalEditors() {
        for (ExternalEditor editor : getExternalEditors()) {
            editor.close();
        }
        reload();
    }

    private List<ExternalEditor> getExternalEditors() {
        return OPENED_EXTERNAL_EDITORS.stream().filter(editor -> editor.accessEquals(getParameterAccess())).collect(Collectors.toList());
    }

    private void openExternalIde() {
        closeExistingExternalEditors();
        ExternalFileExternalEditor externalEditor = new ExternalFileExternalEditor(getWorkbench(),
                new WeakReference<>(getParameterCollection()),
                getParameterAccess().getKey(),
                getParameterTree(), getParameterAccess());
        OPENED_EXTERNAL_EDITORS.add(externalEditor);
        reload();
    }

    private void openIdeInNewWindow() {
        closeExistingExternalEditors();
        WindowExternalEditor externalEditor = new WindowExternalEditor(getWorkbench(),
                new WeakReference<>(getParameterCollection()),
                getParameterAccess().getKey(),
                getParameterTree(), getParameterAccess());
        OPENED_EXTERNAL_EDITORS.add(externalEditor);
        reload();
    }

    private void toggleExternalCode() {
        closeExistingExternalEditors();
        ScriptParameter code = getParameter(ScriptParameter.class);
        code.getExternalScriptFile().setEnabled(!code.getExternalScriptFile().isEnabled());
        setParameter(code, true);
    }

    private void toggleCollapse() {
        ScriptParameter code = getParameter(ScriptParameter.class);
        code.setCollapsed(!code.isCollapsed());
        setParameter(code, true);
    }

    private void openIDEInTab() {
        closeExistingExternalEditors();
        TabExternalEditor externalEditor = new TabExternalEditor(getWorkbench(),
                new WeakReference<>(getParameterCollection()),
                getParameterAccess().getKey(),
                getParameterTree(), getParameterAccess());
        OPENED_EXTERNAL_EDITORS.add(externalEditor);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        ScriptParameter code = getParameter(ScriptParameter.class);
        if (!code.isCollapsed() || !isCollapsed) {
            remove(textArea);
            remove(collapseInfoLabel);
            remove(closeExternalEditorsButton);
            if (pathEditorComponent != null)
                remove(pathEditorComponent);
            if (code.isCollapsed()) {
                add(collapseInfoLabel, BorderLayout.CENTER);
            } else {
                if (code.getExternalScriptFile().isEnabled()) {
                    JIPipeManualParameterAccess access = JIPipeManualParameterAccess.builder().setFieldClass(Path.class)
                            .setGetter(() -> code.getExternalScriptFile().getContent())
                            .setSetter((Object p) -> code.getExternalScriptFile().setContent((Path) p))
                            .setSource(new JIPipeDummyParameterCollection())
                            .setKey("external-path")
                            .addAnnotation(new PathParameterSettings() {
                                @Override
                                public Class<? extends Annotation> annotationType() {
                                    return PathParameterSettings.class;
                                }

                                @Override
                                public PathIOMode ioMode() {
                                    return PathIOMode.Open;
                                }

                                @Override
                                public PathType pathMode() {
                                    return PathType.FilesOnly;
                                }

                                @Override
                                public String[] extensions() {
                                    return new String[0];
                                }

                                @Override
                                public FileChooserSettings.LastDirectoryKey key() {
                                    return FileChooserSettings.LastDirectoryKey.Projects;
                                }
                            }).build();
                    JIPipeParameterEditorUI pathEditor = JIPipe.getInstance().getParameterTypeRegistry().createEditorFor(getWorkbench(), getParameterTree(), access);
                    FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
                    formPanel.addToForm(pathEditor, new JLabel("External script path"), null);
                    add(formPanel, BorderLayout.CENTER);
                    pathEditorComponent = formPanel;
                } else {
                    if(getExternalEditors().isEmpty()) {
                        add(textArea, BorderLayout.CENTER);
                    }
                    else {
                        add(closeExternalEditorsButton, BorderLayout.CENTER);
                    }
                }
            }
            isCollapsed = code.isCollapsed();
            if (!code.getExternalScriptFile().isEnabled() && !Objects.equals(textArea.getText(), code.getCode()))
                textArea.setText(code.getCode());
            revalidate();
            repaint();
        }
    }

    public abstract static class ExternalEditor implements JIPipeWorkbenchAccess {

        private final JIPipeWorkbench workbench;
        private final WeakReference<JIPipeParameterCollection> parameterCollection;
        private final String parameterKey;
        private final JIPipeParameterTree parameterTree;
        private JIPipeParameterAccess parameterAccess;

        public ExternalEditor(JIPipeWorkbench workbench, WeakReference<JIPipeParameterCollection> parameterCollection, String parameterKey, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            this.workbench = workbench;
            this.parameterCollection = parameterCollection;
            this.parameterKey = parameterKey;
            this.parameterTree = parameterTree;
            this.parameterAccess = parameterAccess;
        }

        public JIPipeParameterTree getParameterTree() {
            return parameterTree;
        }

        public WeakReference<JIPipeParameterCollection> getParameterCollection() {
            return parameterCollection;
        }

        public String getParameterKey() {
            return parameterKey;
        }

        public JIPipeParameterAccess getParameterAccess() {
            return parameterAccess;
        }

        public boolean accessEquals(JIPipeParameterAccess access) {
            return this.parameterAccess == access || (access.getSource() == parameterCollection.get() && Objects.equals(access.getKey(), parameterKey));
        }

        public <T> T getParameter(Class<T> klass) {
            return parameterAccess.get(klass);
        }

        @Override
        public JIPipeWorkbench getWorkbench() {
            return workbench;
        }

        public void close() {
            OPENED_EXTERNAL_EDITORS.remove(this);
            parameterAccess = null;
        }
    }

    public static class TabExternalEditor extends ExternalEditor {
        private  LargeScriptParameterEditorUI editorUI;

        public TabExternalEditor(JIPipeWorkbench workbench, WeakReference<JIPipeParameterCollection> parameterCollection, String parameterKey, JIPipeParameterTree parameterTree, JIPipeParameterAccess access) {
            super(workbench, parameterCollection, parameterKey, parameterTree, access);
            initialize();
        }

        private void initialize() {
            editorUI = new LargeScriptParameterEditorUI(getWorkbench(), getParameterTree(), getParameterAccess());
            ScriptParameter code = getParameter(ScriptParameter.class);
            getWorkbench().getDocumentTabPane().addTab(getParameterAccess().getName() + " (" + code.getLanguageName() + ")",
                    UIUtils.getIconFromResources("actions/dialog-xml-editor.png"),
                    editorUI,
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    true);
            getWorkbench().getDocumentTabPane().switchToLastTab();
        }

        @Override
        public void close() {
            super.close();
            DocumentTabPane.DocumentTab tab = getWorkbench().getDocumentTabPane().getTabContainingContent(editorUI);
            if(tab != null) {
                getWorkbench().getDocumentTabPane().closeTab(tab);
            }
            editorUI = null;
        }
    }

    public static class WindowExternalEditor extends ExternalEditor {

        private JFrame frame;

        public WindowExternalEditor(JIPipeWorkbench workbench, WeakReference<JIPipeParameterCollection> parameterCollection, String parameterKey, JIPipeParameterTree parameterTree, JIPipeParameterAccess access) {
            super(workbench, parameterCollection, parameterKey, parameterTree, access);
            initialize();
        }

        private void initialize() {
            LargeScriptParameterEditorUI editorUI = new LargeScriptParameterEditorUI(getWorkbench(), getParameterTree(), getParameterAccess());
            ScriptParameter code = getParameter(ScriptParameter.class);
            frame = new JFrame();
            frame.setTitle("JIPipe - " + getParameterAccess().getKey() + " (" + code.getLanguageName() + ")");
            frame.setContentPane(editorUI);
            frame.setIconImage(UIUtils.getJIPipeIcon128());
            frame.pack();
            frame.setSize(1024,768);
            frame.setLocationRelativeTo(getWorkbench().getWindow());
            frame.setVisible(true);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    WindowExternalEditor.super.close();
                }
            });
        }

        @Override
        public void close() {
            super.close();
            frame.setVisible(false);
            frame = null;
        }
    }

    public static class ExternalFileExternalEditor extends ExternalEditor {

        private Path targetDirectory;
        private Path targetFile;
        private WatchService watchService;

        private Timer timer;

        public ExternalFileExternalEditor(JIPipeWorkbench workbench, WeakReference<JIPipeParameterCollection> parameterCollection, String parameterKey, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            super(workbench, parameterCollection, parameterKey, parameterTree, parameterAccess);
            initialize();
        }

        private void initialize() {
            ScriptParameter parameter = getParameter(ScriptParameter.class);
            targetDirectory = RuntimeSettings.generateTempDirectory("script-editor");
            targetFile = targetDirectory.resolve("script" + parameter.getExtension());
            try {
                Files.write(targetFile, StringUtils.nullToEmpty(parameter.getCode()).getBytes(StandardCharsets.UTF_8));
                watchService = FileSystems.getDefault().newWatchService();
                targetDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            timer = new Timer(1000, e -> updateWatchService());
            timer.setRepeats(true);
            timer.start();

            // Open the standard editor
            try {
                Desktop.getDesktop().open(targetFile.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void updateWatchService() {
            WatchKey key = watchService.poll();
            if (key != null) {
                boolean changed = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if(kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        changed = true;
                    }
                }
                key.reset();
                if(changed) {
                    updateScriptFromFile();
                }
            }
        }

        private void updateScriptFromFile() {
            try {
                String code = new String(Files.readAllBytes(targetFile), StandardCharsets.UTF_8);
                ScriptParameter parameter = getParameter(ScriptParameter.class);
                parameter.setCode(code);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void close() {
            try {
                timer.stop();
                watchService.close();
                updateScriptFromFile();
                SwingUtilities.invokeLater(() -> PathUtils.deleteDirectoryRecursively(targetDirectory, new JIPipeProgressInfo()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            super.close();
        }
    }
}
