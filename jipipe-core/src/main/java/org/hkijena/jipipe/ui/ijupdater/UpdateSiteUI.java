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

package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.utils.ModernMetalTheme;
import org.hkijena.jipipe.utils.RoundedLineBorder;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class UpdateSiteUI extends JPanel {
    private final UpdateSiteListUI listUI;
    private final UpdateSite updateSite;

    public UpdateSiteUI(UpdateSiteListUI listUI, UpdateSite updateSite) {
        this.listUI = listUI;
        this.updateSite = updateSite;
        initialize();
    }

    private void initialize() {
        setBorder(new RoundedLineBorder(ModernMetalTheme.MEDIUM_GRAY, 1, 2));
        setLayout(new GridBagLayout());

        JCheckBox enabledCheckBox = new JCheckBox(updateSite.getName(), updateSite.isActive());
        enabledCheckBox.addActionListener(e -> {
            if(listUI.getPluginManager().isCurrentlyRunning()) {
                JOptionPane.showMessageDialog(this,
                        "There is already an operation running. Please wait until it is finished.",
                        "Activate/deactivate update site", JOptionPane.ERROR_MESSAGE);
                enabledCheckBox.setSelected(!enabledCheckBox.isSelected());
            }
            else {
                if (enabledCheckBox.isSelected()) {
                    activateUpdateSite();
                } else {
                    deactivateUpdateSite();
                }
            }
        });
        enabledCheckBox.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        add(enabledCheckBox, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                weightx = 1;
                fill = HORIZONTAL;
            }
        });
        if(updateSite.isOfficial()) {
            JLabel officialLabel = new JLabel("Official");
            officialLabel.setForeground(new Color(0,128,0));
            add(officialLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 0;
                    insets = UIUtils.UI_PADDING;
                    anchor = GridBagConstraints.EAST;
                }
            });
        }

        JTextArea descriptionArea = UIUtils.makeReadonlyBorderlessTextArea(updateSite.getDescription());
        add(descriptionArea, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 1;
                gridwidth = 2;
                weightx = 1;
                fill = HORIZONTAL;
            }
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());

        JTextField urlField = UIUtils.makeReadonlyBorderlessTextField(updateSite.getURL());
        urlField.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        bottomPanel.add(urlField, BorderLayout.CENTER);

        JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.addActionListener(e -> removeUpdateSite());
        UIUtils.makeFlat25x25(removeButton);
        bottomPanel.add(removeButton, BorderLayout.WEST);

        add(bottomPanel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 2;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
    }

    private void deactivateUpdateSite() {
        listUI.getPluginManager().deactivateUpdateSite(updateSite);
    }

    private void activateUpdateSite() {
        listUI.getPluginManager().stageActivateUpdateSite(updateSite);
    }

    private void removeUpdateSite() {
        if(JOptionPane.showConfirmDialog(this, "Do you really want to remove the update site '" + updateSite.getName() + "'?",
                "Remove update site",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            listUI.getPluginManager().removeUpdateSite(updateSite);
            listUI.refreshList();
        }
    }
}
