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

package org.hkijena.jipipe.plugins.parameters.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.List;

/**
 * Adds an entry "New table" to the JIPipe menu
 */
public class FileChooserTesterJIPipeDesktopMenuExtension extends JIPipeDesktopMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public FileChooserTesterJIPipeDesktopMenuExtension(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Test file choosers");
        setToolTipText("Tests file choosers");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/bugs.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame frame = new JFrame();
        frame.setTitle("Test file choosers");
        JPanel buttonPanel = new JPanel(new GridLayout(3,3));
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(buttonPanel, BorderLayout.NORTH);

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new  Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textPane);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // Open single
        buttonPanel.add(UIUtils.createButton("OPEN single file zip/jip/png", JIPipe.RESOURCES.getIcon16("actions/fileopen.png"), () -> {
            Path result = JIPipeDesktop.selectSingle(frame,
                    getDesktopWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Open single file",
                    HTMLText.EMPTY,
                    PathIOMode.Open,
                    PathType.FilesOnly,
                    PathUtils.EXTENSION_FILTER_ZIP,
                    PathUtils.EXTENSION_FILTER_JIP,
                    PathUtils.EXTENSION_FILTER_PNG);
            textPane.setText(JsonUtils.toPrettyJsonString(result));
        }));
        buttonPanel.add(UIUtils.createButton("OPEN single dir", JIPipe.RESOURCES.getIcon16("actions/fileopen.png"), () -> {
            Path result = JIPipeDesktop.selectSingle(frame,
                    getDesktopWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Open single dir",
                    HTMLText.EMPTY,
                    PathIOMode.Open,
                    PathType.DirectoriesOnly);
            textPane.setText(JsonUtils.toPrettyJsonString(result));
        }));
        buttonPanel.add(UIUtils.createButton("OPEN single path", JIPipe.RESOURCES.getIcon16("actions/fileopen.png"), () -> {
            Path result = JIPipeDesktop.selectSingle(frame,
                    getDesktopWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Open single path",
                    HTMLText.EMPTY,
                    PathIOMode.Open,
                    PathType.FilesAndDirectories);
            textPane.setText(JsonUtils.toPrettyJsonString(result));
        }));

        // Open multi
        buttonPanel.add(UIUtils.createButton("OPEN multiple file zip/jip/png", JIPipe.RESOURCES.getIcon16("actions/fileopen.png"), () -> {
            List<Path> result = JIPipeDesktop.selectMulti(frame,
                    getDesktopWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Open multiple file",
                    HTMLText.EMPTY,
                    PathIOMode.Open,
                    PathType.FilesOnly,
                    PathUtils.EXTENSION_FILTER_ZIP,
                    PathUtils.EXTENSION_FILTER_JIP,
                    PathUtils.EXTENSION_FILTER_PNG);
            textPane.setText(JsonUtils.toPrettyJsonString(result));
        }));
        buttonPanel.add(UIUtils.createButton("OPEN multiple dir", JIPipe.RESOURCES.getIcon16("actions/fileopen.png"), () -> {
            List<Path> result = JIPipeDesktop.selectMulti(frame,
                    getDesktopWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Open multiple dir",
                    HTMLText.EMPTY,
                    PathIOMode.Open,
                    PathType.DirectoriesOnly);
            textPane.setText(JsonUtils.toPrettyJsonString(result));
        }));
        buttonPanel.add(UIUtils.createButton("OPEN multiple path", JIPipe.RESOURCES.getIcon16("actions/fileopen.png"), () -> {
            List<Path> result = JIPipeDesktop.selectMulti(frame,
                    getDesktopWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Open multiple path",
                    HTMLText.EMPTY,
                    PathIOMode.Open,
                    PathType.FilesAndDirectories);
            textPane.setText(JsonUtils.toPrettyJsonString(result));
        }));

        // Saving
        buttonPanel.add(UIUtils.createButton("SAVE single file zip/jip/png", JIPipe.RESOURCES.getIcon16("actions/stock_save.png"), () -> {
            Path result = JIPipeDesktop.selectSingle(frame,
                    getDesktopWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Save single file",
                    HTMLText.EMPTY,
                    PathIOMode.Save,
                    PathType.FilesOnly,
                    PathUtils.EXTENSION_FILTER_ZIP,
                    PathUtils.EXTENSION_FILTER_JIP,
                    PathUtils.EXTENSION_FILTER_PNG);
            textPane.setText(JsonUtils.toPrettyJsonString(result));
        }));
        buttonPanel.add(UIUtils.createButton("SAVE single dir", JIPipe.RESOURCES.getIcon16("actions/stock_save.png"), () -> {
            Path result = JIPipeDesktop.selectSingle(frame,
                    getDesktopWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Save single dir",
                    HTMLText.EMPTY,
                    PathIOMode.Save,
                    PathType.DirectoriesOnly);
            textPane.setText(JsonUtils.toPrettyJsonString(result));
        }));
        buttonPanel.add(UIUtils.createButton("SAVE single path", JIPipe.RESOURCES.getIcon16("actions/stock_save.png"), () -> {
            Path result = JIPipeDesktop.selectSingle(frame,
                    getDesktopWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.External,
                    "Save single path",
                    HTMLText.EMPTY,
                    PathIOMode.Save,
                    PathType.FilesAndDirectories);
            textPane.setText(JsonUtils.toPrettyJsonString(result));
        }));

        frame.pack();
        frame.setSize(1027,768);
        frame.setLocationRelativeTo(getDesktopProjectWorkbench().getWindow());
        frame.setVisible(true);
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Development";
    }
}
