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

package org.hkijena.jipipe.ui.components.search;

import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A {@link org.jdesktop.swingx.JXTextField} designed for searching
 */
public class SearchTextField extends JPanel implements Predicate<String> {

    private final JXTextField textField = new JXTextField();
    private String[] searchStrings = new String[0];
    private JPanel buttonPanel = new JPanel();

    /**
     * Creates a new instance
     */
    public SearchTextField() {
        setLayout(new BorderLayout(4, 0));
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(UIUtils.createControlBorder());

        textField.setPrompt("Search ...");
        textField.setBorder(null);
        add(textField, BorderLayout.CENTER);

        buttonPanel.setBackground(UIManager.getColor("TextField.background"));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        add(buttonPanel, BorderLayout.EAST);
        addButton("Clear search", UIUtils.getIconFromResources("actions/edit-clear.png"), (searchTextField) -> searchTextField.setText(""));
    }

    public void addButton(String name, Icon icon, Consumer<SearchTextField> action) {
        JButton button = new JButton(icon);
        button.setToolTipText(name);
        button.setOpaque(false);
        button.addActionListener(e -> action.accept(this));
        UIUtils.makeFlat25x25(button);
        button.setBorder(null);
        buttonPanel.add(button);
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
