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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Predicate;

/**
 * A {@link org.jdesktop.swingx.JXTextField} designed for searching
 */
public class SearchTextField extends JPanel implements Predicate<String> {

    private final JXTextField textField = new JXTextField();
    private String[] searchStrings = new String[0];

    /**
     * Creates a new instance
     */
    public SearchTextField() {
        setLayout(new BorderLayout(4, 0));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEtchedBorder());

        textField.setPrompt("Search ...");
        textField.setBorder(null);
        add(textField, BorderLayout.CENTER);

        JButton clearButton = new JButton(UIUtils.getIconFromResources("actions/edit-clear.png"));
        clearButton.setOpaque(false);
        clearButton.setToolTipText("Clear");
        clearButton.addActionListener(e -> setText(""));
        UIUtils.makeFlat25x25(clearButton);
        clearButton.setBorder(null);
        add(clearButton, BorderLayout.EAST);

    }

    /**
     * Returns the search strings according to the current text
     *
     * @return the search strings
     */
    public String[] getSearchStrings() {
        return searchStrings;
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public JXTextField getTextField() {
        return textField;
    }

    /**
     * Adds a listener for when the search text changes
     *
     * @param listener the listener
     */
    public void addActionListener(ActionListener listener) {
        textField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                updateSearchStrings();
                listener.actionPerformed(new ActionEvent(this, 1, "search-text-changed"));
            }
        });
    }

    private void updateSearchStrings() {
        if (getText() != null) {
            String str = getText().trim();
            if (!str.isEmpty()) {
                searchStrings = str.split(" ");
            } else {
                searchStrings = new String[0];
            }
        } else {
            searchStrings = new String[0];
        }
    }

    @Override
    public boolean test(String s) {
        if (s == null)
            s = "";
        for (String searchString : getSearchStrings()) {
            if (!s.toLowerCase().contains(searchString.toLowerCase()))
                return false;
        }
        return true;
    }
}
