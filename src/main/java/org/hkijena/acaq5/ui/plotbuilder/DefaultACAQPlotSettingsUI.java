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

package org.hkijena.acaq5.ui.plotbuilder;


import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DefaultACAQPlotSettingsUI extends ACAQPlotSettingsUI {

    private int gridBagRow = 0;

    public DefaultACAQPlotSettingsUI(ACAQPlot plot) {
        super(plot);
        setLayout(new GridBagLayout());
        initialize();
    }

    private void initialize() {
        addStringEditorComponent("Title", () -> getPlot().getTitle(), s -> getPlot().setTitle(s));
    }

    protected void addComponent(String label, Icon icon, Component component) {
        final int finalRow = gridBagRow++;
        add(new JLabel(label, icon, JLabel.LEFT), new GridBagConstraints() {
            {
                gridx = 0;
                gridy = finalRow;
                anchor = GridBagConstraints.WEST;
                insets = UIUtils.UI_PADDING;
            }
        });
        add(component, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = finalRow;
                anchor = GridBagConstraints.WEST;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
                insets = UIUtils.UI_PADDING;
            }
        });
    }

    protected void addStringEditorComponent(String label, Supplier<String> getter, Consumer<String> setter) {
        JTextField textField = new JTextField(getter.get());
        textField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                setter.accept("" + textField.getText());
            }
        });
        addComponent(label, UIUtils.getIconFromResources("text.png"), textField);
    }
}
