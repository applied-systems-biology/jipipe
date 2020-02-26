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

package org.hkijena.acaq5.ui.samplemanagement;

import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Allows adding one or multiple samples manually by providing name(s)
 */
public class ACAQAddSamplesDialog extends JDialog implements WindowListener {

    private ACAQWorkbenchUI workbenchUI;
    private JTextArea samplesInput;

    public ACAQAddSamplesDialog(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI.getWindow());
        this.workbenchUI = workbenchUI;
        initialize();
        addWindowListener(this);
    }

    private void initialize() {
        setSize(400, 300);
        getContentPane().setLayout(new BorderLayout(8, 8));
        setTitle("Add samples");
        setIconImage(UIUtils.getIconFromResources("module.png").getImage());

        JTextArea infoArea = new JTextArea("Please insert the name of the sample. You can also add multiple samples at once by writing multiple lines. Each line represents one sample.");
        infoArea.setEditable(false);
        infoArea.setOpaque(false);
        infoArea.setBorder(null);
        infoArea.setWrapStyleWord(true);
        infoArea.setLineWrap(true);
        add(infoArea, BorderLayout.NORTH);

        samplesInput = new JTextArea();
        add(new JScrollPane(samplesInput), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(cancelButton);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addFromInput());
        buttonPanel.add(addButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addFromInput() {
        if(samplesInput.getText() != null && !samplesInput.getText().isEmpty()) {
            for(String line : samplesInput.getText().split("\n")) {
                String modified = line.trim();
                if(!modified.isEmpty()) {
                    if(!workbenchUI.getProject().getSamples().containsKey(modified)) {
                        workbenchUI.getProject().getOrCreate(modified);
                    }
                }
            }
        }
        setVisible(false);
    }

    @Override
    public void windowOpened(WindowEvent e) {
        samplesInput.requestFocus();
    }

    @Override
    public void windowClosing(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
