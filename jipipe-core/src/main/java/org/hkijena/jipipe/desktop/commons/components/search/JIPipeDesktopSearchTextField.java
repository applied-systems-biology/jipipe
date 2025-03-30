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

package org.hkijena.jipipe.desktop.commons.components.search;

import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.utils.ColorUtils;
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
public class JIPipeDesktopSearchTextField extends JPanel implements Predicate<String> {

    public static final int ANIMATION_DELAY = 80;
    public static final double ANIMATION_SPEED = 0.05;
    private final JTextField textField = new JTextField();
    private final JPanel buttonPanel = new JPanel();
    private final Timer attentionAnimationTimer;
    private String[] searchStrings = new String[0];
    private double attentionAnimationStatus = 1;

    /**
     * Creates a new instance
     */
    public JIPipeDesktopSearchTextField() {
        this.attentionAnimationTimer = new Timer(ANIMATION_DELAY, e -> updateAttentionAnimation());
        this.attentionAnimationTimer.setRepeats(true);
        this.attentionAnimationTimer.setCoalesce(false);
        initialize();
    }

    private void updateAttentionAnimation() {
        if (!isDisplayable() || attentionAnimationStatus >= 1) {
            attentionAnimationTimer.stop();
            setBorder(UIUtils.createControlBorder());
        } else {
            Color borderColor = ColorUtils.mix(UIUtils.COLOR_SUCCESS, UIUtils.getControlBorderColor(), attentionAnimationStatus);
            setBorder(UIUtils.createControlBorder(borderColor));
            attentionAnimationStatus += ANIMATION_SPEED;

            repaint();
            getToolkit().sync();
        }
    }

    private void initialize() {
        setLayout(new BorderLayout(4, 0));
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(UIUtils.createControlBorder());


        JButton searchButton = new JButton(UIUtils.getIconInvertedFromResources("actions/search.png"));
        searchButton.addActionListener(e -> { textField.requestFocusInWindow(); textField.selectAll(); });
        UIUtils.makeButtonFlat25x25(searchButton);
        searchButton.setRequestFocusEnabled(false);
        searchButton.setFocusable(false);

        add(searchButton, BorderLayout.WEST);

        textField.setBorder(null);
        add(textField, BorderLayout.CENTER);

        buttonPanel.setBackground(UIManager.getColor("TextField.background"));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        add(buttonPanel, BorderLayout.EAST);
        addButton("Clear search", UIUtils.getIconFromResources("actions/edit-clear.png"), (searchTextField) -> {
            clear();
        });
    }

    public void clear() {
        setText("");
        textField.requestFocusInWindow();
    }

    public void addButton(String name, Icon icon, Consumer<JIPipeDesktopSearchTextField> action) {
        JButton button = new JButton(icon);
        button.setToolTipText(name);
        button.setOpaque(false);
        button.addActionListener(e -> action.accept(this));
        UIUtils.makeButtonFlat25x25(button);
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

    public JTextField getTextField() {
        return textField;
    }

    /**
     * Adds a listener for when the search text changes
     *
     * @param listener the listener
     */
    public void addActionListener(ActionListener listener) {
        textField.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
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

    public void grabAttentionAnimation() {
        attentionAnimationStatus = 0;
        attentionAnimationTimer.restart();
    }
}
