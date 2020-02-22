/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.components;

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
public class FileSelection extends JPanel {

    private JFileChooser fileChooser = new JFileChooser();
    private JTextField pathEdit;
    private IOMode ioMode;
    private PathMode pathMode;
    private Set<ActionListener> listeners = new HashSet<>();

    public FileSelection() {
        this.ioMode = IOMode.Open;
        setPathMode(PathMode.FilesOnly);
        initialize();
    }

    public FileSelection(IOMode ioMode, PathMode pathMode) {
        this.ioMode = ioMode;
        setPathMode(pathMode);
        initialize();
    }

    private void initialize() {
        // Setup the GUI
        setLayout(new GridBagLayout());

        pathEdit = new JTextField();
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

        JButton selectButton = new JButton(UIUtils.getIconFromResources("open.png"));
        add(selectButton, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.PAGE_START;
                gridx = 1;
                gridy = 0;
            }
        });

        selectButton.addActionListener(actionEvent -> {
            if(ioMode == ioMode.Open) {
                if(getFileChooser().showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    pathEdit.setText(getFileChooser().getSelectedFile().toString());
                }
            }
            else {
                if(getFileChooser().showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    pathEdit.setText(getFileChooser().getSelectedFile().toString());
                }
            }
        });

        pathEdit.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                postAction();
            }
        });
    }

    public void setPath(Path path) {
        if(path != null)
            pathEdit.setText(path.toString());
        else
            pathEdit.setText("");
    }

    public Path getPath() {
        return Paths.get(pathEdit.getText());
    }

    public JFileChooser getFileChooser() {
        return fileChooser;
    }

    private void postAction() {
        for(ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(this, 0, "text-changed"));
        }
    }

    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }

    public IOMode getIoMode() {
        return ioMode;
    }

    public void setIoMode(IOMode ioMode) {
        this.ioMode = ioMode;
    }

    public PathMode getPathMode() {
        return pathMode;
    }

    public void setPathMode(PathMode pathMode) {
        this.pathMode = pathMode;
        switch (pathMode) {
            case FilesOnly:
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                break;
            case DirectoriesOnly:
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                break;
            case FilesAndDirectories:
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                break;
        }
    }

    public enum IOMode {
        Open,
        Save
    }

    public enum PathMode {
        FilesOnly,
        DirectoriesOnly,
        FilesAndDirectories
    }
}
