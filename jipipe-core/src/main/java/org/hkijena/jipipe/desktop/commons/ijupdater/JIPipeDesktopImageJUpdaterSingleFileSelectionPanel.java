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

package org.hkijena.jipipe.desktop.commons.ijupdater;

import com.google.common.collect.ImmutableList;
import net.imagej.updater.FileObject;
import net.imagej.updater.GroupAction;
import org.apache.commons.text.WordUtils;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Opened when selecting one file
 */
public class JIPipeDesktopImageJUpdaterSingleFileSelectionPanel extends JIPipeDesktopWorkbenchPanel {

    public static final String[] months = {"Zero", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private final FileObject fileObject;
    private final JIPipeDesktopImageJUpdaterManagerUI managerUI;
    private final JIPipeDesktopFormPanel actionButtons = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
    private JLabel statusLabel;
    private JLabel actionLabel;

    /**
     * @param workbench  the workbench
     * @param managerUI  the manager
     * @param fileObject the object to be displayed
     */
    public JIPipeDesktopImageJUpdaterSingleFileSelectionPanel(JIPipeDesktopWorkbench workbench, JIPipeDesktopImageJUpdaterManagerUI managerUI, FileObject fileObject) {
        super(workbench);
        this.managerUI = managerUI;
        this.fileObject = fileObject;
        initialize();
        refreshContents();
    }

    private String prettyPrintTimestamp(final long timestamp) {
        final String t = "" + timestamp + "00000000";
        return t.substring(6, 8) + " " +
                months[Integer.parseInt(t.substring(4, 6))] + " " + t.substring(0, 4);
    }

    private void refreshContents() {
        statusLabel.setText(WordUtils.capitalizeFully(fileObject.getStatus().toString().replace("_", " ")));
        actionLabel.setText(fileObject.getAction().toString());

        actionButtons.clear();
        Set<FileObject> selected = Collections.singleton(fileObject);
        for (GroupAction action : managerUI.getFilesCollection().getValidActions(selected)) {
            JButton button = new JButton(action.getLabel(managerUI.getFilesCollection(), selected));
            button.addActionListener(e -> {
                action.setAction(managerUI.getFilesCollection(), fileObject);
                managerUI.fireFileChanged(fileObject);
                refreshContents();
            });
            actionButtons.addWideToForm(button, null);
        }

    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
        statusLabel = new JLabel();
        actionLabel = new JLabel();

        formPanel.addGroupHeader("Current status", UIUtils.getIconFromResources("actions/configure.png"));
        formPanel.addToForm(statusLabel, new JLabel("Status"), null);
        formPanel.addToForm(actionLabel, new JLabel("Action"), null);

        formPanel.addWideToForm(actionButtons, null);

        formPanel.addGroupHeader("General information", UIUtils.getIconFromResources("actions/configure.png"));
        formPanel.addToForm(UIUtils.makeReadonlyBorderlessTextField(fileObject.getFilename()), new JLabel("File name"), null);
        formPanel.addToForm(UIUtils.makeReadonlyBorderlessTextField(fileObject.getLocalFilename(true)), new JLabel("Local file name"), null);
        formPanel.addToForm(UIUtils.makeReadonlyBorderlessTextField(prettyPrintTimestamp(fileObject.getTimestamp())), new JLabel("Release data"), null);
        formPanel.addToForm(UIUtils.makeReadonlyBorderlessTextArea(fileObject.getDescription()), new JLabel("Description"), null);
        formPanel.addToForm(UIUtils.makeReadonlyBorderlessTextArea(String.join("\n", fileObject.getCategories())), new JLabel("Categories"), null);
        formPanel.addToForm(UIUtils.makeReadonlyBorderlessTextArea(String.join("\n", fileObject.getAuthors())), new JLabel("Authors"), null);
        ImmutableList<String> urls = ImmutableList.copyOf(fileObject.getLinks());
        if (!urls.isEmpty()) {
            Map<TextAttribute, Integer> fontAttributes = new HashMap<>();
            fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            Font font = new Font(Font.DIALOG, Font.PLAIN, 12).deriveFont(fontAttributes);
            for (int i = 0; i < urls.size(); i++) {
                JButton linkButton = new JButton(urls.get(i));
                linkButton.setBorder(null);
                linkButton.setFont(font);
                linkButton.setForeground(Color.BLUE);
                int finalI = i;
                linkButton.addActionListener(e -> UIUtils.openWebsite(urls.get(finalI)));
                if (i == 0) {
                    formPanel.addToForm(linkButton, new JLabel("Website"), null);
                } else {
                    formPanel.addToForm(linkButton, null, null);
                }
            }
        }

        formPanel.addGroupHeader("Dependencies", UIUtils.getIconFromResources("actions/configure.png"));
        formPanel.addWideToForm(UIUtils.makeReadonlyBorderlessTextArea(ImmutableList.copyOf(fileObject.getFileDependencies(
                managerUI.getFilesCollection(), true
        )).stream().map(FileObject::getFilename).collect(Collectors.joining("\n"))), null);

        formPanel.addVerticalGlue();
        add(formPanel, BorderLayout.CENTER);
    }
}
