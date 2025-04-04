/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * Text field with a file selection
 */
public class JIPipeDesktopPathEditorComponent extends JIPipeDesktopWorkbenchPanel {

    private final Set<ActionListener> listeners = new HashSet<>();
    private JTextField pathEdit;
    private PathIOMode ioMode;
    private PathType pathMode;
    private JButton generateRandomButton;
    private List<FileNameExtensionFilter> extensionFilters = new ArrayList<>();
    private JIPipeFileChooserApplicationSettings.LastDirectoryKey directoryKey = JIPipeFileChooserApplicationSettings.LastDirectoryKey.Parameters;

    /**
     * Creates a new file selection that opens a file
     */
    public JIPipeDesktopPathEditorComponent(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setPathMode(PathType.FilesOnly);
        initialize();
        setIoMode(PathIOMode.Open);
    }

    /**
     * @param ioMode   If a path is opened or saved
     * @param pathMode If the path is a file, directory or anything
     */
    public JIPipeDesktopPathEditorComponent(JIPipeDesktopWorkbench workbench, PathIOMode ioMode, PathType pathMode) {
        super(workbench);
        setPathMode(pathMode);
        initialize();
        setIoMode(ioMode);
    }

    private void initialize() {
        // Setup the GUI
        setBackground(UIManager.getColor("TextArea.background"));
        setBorder(UIUtils.createControlBorder());
        setLayout(new GridBagLayout());

        pathEdit = new JTextField();
        pathEdit.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        pathEdit.setBorder(null);
        add(pathEdit, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridwidth = 1;
                anchor = GridBagConstraints.WEST;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
                insets = UIUtils.UI_PADDING;
            }
        });

        generateRandomButton = new JButton(UIUtils.getIconFromResources("actions/random.png"));
        generateRandomButton.setToolTipText("Generate random file or folder");
        UIUtils.makeButtonFlat25x25(generateRandomButton);
        generateRandomButton.setBorder(null);
        generateRandomButton.addActionListener(e -> generateRandom());
        add(generateRandomButton, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.PAGE_START;
                gridx = 1;
                gridy = 0;
            }
        });

        JButton selectButton = new JButton("Select", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        selectButton.setToolTipText("Select from filesystem");
        add(selectButton, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.PAGE_START;
                gridx = 2;
                gridy = 0;
            }
        });

        selectButton.addActionListener(e -> choosePath());

        pathEdit.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                postAction();
            }
        });
    }

    /**
     * Opens the file chooser
     */
    public void choosePath() {
        Path selected = JIPipeDesktop.selectSingle(this,
                getDesktopWorkbench(),
                directoryKey,
                "Change current value",
                HTMLText.EMPTY, ioMode,
                pathMode,
                extensionFilters.toArray(new FileNameExtensionFilter[0]));
        if (selected != null)
            pathEdit.setText(selected.toString());
    }

    private void generateRandom() {
        if (pathMode == PathType.DirectoriesOnly) {
            setPath(JIPipeRuntimeApplicationSettings.getTemporaryDirectory("tmp"));
        } else {
            setPath(JIPipeRuntimeApplicationSettings.getTemporaryFile("tmp", null));
        }
    }

    public Path getPath() {
        try {
            return Paths.get(pathEdit.getText());
        } catch (Exception e) {
            e.printStackTrace();
            return Paths.get("");
        }
    }

    public void setPath(Path path) {
        if (path != null) {
            String current = StringUtils.orElse(pathEdit.getText(), "");
            if (!Objects.equals(current, path.toString())) {
                pathEdit.setText(path.toString());
            }
        } else
            pathEdit.setText("");
    }

    private void postAction() {
        for (ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(this, 0, "text-changed"));
        }
    }

    /**
     * Adds a listener for when the path property changes
     *
     * @param listener Listens to when a file is selected
     */
    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    /**
     * @param listener Registered listener
     */
    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }

    public PathIOMode getIoMode() {
        return ioMode;
    }

    public void setIoMode(PathIOMode ioMode) {
        this.ioMode = ioMode;
        generateRandomButton.setVisible(ioMode == PathIOMode.Save);
    }

    public PathType getPathMode() {
        return pathMode;
    }

    public void setPathMode(PathType pathMode) {
        this.pathMode = pathMode;
    }

    public List<FileNameExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    public void setExtensionFilters(List<FileNameExtensionFilter> extensionFilters) {
        this.extensionFilters = extensionFilters;
    }


    public void setExtensionFilters(String[] extensions) {
        extensionFilters.clear();
        for (String extension : extensions) {
            if ("csv".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_CSV);
            else if ("png".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_PNG);
            else if ("svg".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_SVG);
            else if ("md".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_MD);
            else if ("pdf".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_PDF);
            else if ("html".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_HTML);
            else if ("jpeg".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_JPEG);
            else if ("jip".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_JIP);
            else if ("jipe".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_JIPE);
            else if ("jipc".equals(extension))
                extensionFilters.add(UIUtils.EXTENSION_FILTER_JIPC);
            else
                extensionFilters.add(new FileNameExtensionFilter(extension.toUpperCase(Locale.ROOT) + " file (*." + extension + ")", extension));
        }
    }

    public JIPipeFileChooserApplicationSettings.LastDirectoryKey getDirectoryKey() {
        return directoryKey;
    }

    public void setDirectoryKey(JIPipeFileChooserApplicationSettings.LastDirectoryKey directoryKey) {
        this.directoryKey = directoryKey;
    }

}
