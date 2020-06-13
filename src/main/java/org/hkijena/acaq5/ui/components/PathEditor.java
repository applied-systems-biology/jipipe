/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.extensions.settings.FileChooserSettings;
import org.hkijena.acaq5.extensions.settings.RuntimeSettings;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Text field with a file selection
 */
public class PathEditor extends JPanel {

    private JTextField pathEdit;
    private IOMode ioMode;
    private PathMode pathMode;
    private Set<ActionListener> listeners = new HashSet<>();
    private JButton generateRandomButton;

    /**
     * Creates a new file selection that opens a file
     */
    public PathEditor() {
        setPathMode(PathMode.FilesOnly);
        initialize();
        setIoMode(IOMode.Open);
    }

    /**
     * @param ioMode   If a path is opened or saved
     * @param pathMode If the path is a file, directory or anything
     */
    public PathEditor(IOMode ioMode, PathMode pathMode) {
        setPathMode(pathMode);
        initialize();
        setIoMode(ioMode);
    }

    private void initialize() {
        // Setup the GUI
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEtchedBorder());
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

        generateRandomButton = new JButton(UIUtils.getIconFromResources("random.png"));
        generateRandomButton.setToolTipText("Generate random file or folder");
        UIUtils.makeFlat25x25(generateRandomButton);
        generateRandomButton.setBorder(null);
        generateRandomButton.addActionListener(e -> generateRandom());
        add(generateRandomButton, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.PAGE_START;
                gridx = 1;
                gridy = 0;
            }
        });

        JButton selectButton = new JButton(UIUtils.getIconFromResources("open.png"));
        selectButton.setToolTipText("Select from filesystem");
        UIUtils.makeFlat25x25(selectButton);
        selectButton.setBorder(null);
        add(selectButton, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.PAGE_START;
                gridx = 2;
                gridy = 0;
            }
        });

        selectButton.addActionListener(e -> choosePath());

        pathEdit.getDocument().addDocumentListener(new DocumentChangeListener() {
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
        Path selected = FileChooserSettings.selectSingle(this,
                FileChooserSettings.KEY_PARAMETER,
                "Change current value",
                ioMode,
                pathMode);
        if (selected != null)
            pathEdit.setText(selected.toString());
    }

    private void generateRandom() {
        if (pathMode == PathMode.DirectoriesOnly) {
            setPath(RuntimeSettings.generateTempDirectory("ACAQ5"));
        } else {
            setPath(RuntimeSettings.generateTempFile("ACAQ5", null));
        }
    }

    public Path getPath() {
        return Paths.get(pathEdit.getText());
    }

    public void setPath(Path path) {
        if (path != null)
            pathEdit.setText(path.toString());
        else
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

    public IOMode getIoMode() {
        return ioMode;
    }

    public void setIoMode(IOMode ioMode) {
        this.ioMode = ioMode;
        generateRandomButton.setVisible(ioMode == IOMode.Save);
    }

    public PathMode getPathMode() {
        return pathMode;
    }

    public void setPathMode(PathMode pathMode) {
        this.pathMode = pathMode;
    }

    /**
     * Determines if a path is opened or saved
     */
    public enum IOMode {
        Open,
        Save
    }

    /**
     * Determines the type of selected path
     */
    public enum PathMode {
        FilesOnly,
        DirectoriesOnly,
        FilesAndDirectories
    }
}
