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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.desktop.commons.components.textfield.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A {@link org.jdesktop.swingx.JXTextField} designed for searching
 */
public class JIPipeDesktopSearchTextField extends JPanel implements Predicate<String>, JIPipeRunnable.StartedEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    public static final int ANIMATION_DELAY = 80;
    public static final double ANIMATION_SPEED = 0.05;
    public static final int SEARCH_DEBOUNCE_DELAY = 150;
    private final JTextField textField = new JTextField();
    private final JPanel buttonPanel = new JPanel();
    private final Timer attentionAnimationTimer;
    private final JButton searchButton = new JButton();
    private final JIPipeRunnableExecutor queue;
    private final Icon readyIcon = JIPipe.RESOURCES.getIcon16Inverted("actions/search.png");
    private final SpinnerIcon busyIcon = new SpinnerIcon(searchButton);
    private String[] searchStrings = new String[0];
    private double attentionAnimationStatus = 1;
    private boolean isProgrammaticFocusChange = false;
    private StaticDebouncer searchDebouncer;

    public JIPipeDesktopSearchTextField() {
        this(null);
    }

    public JIPipeDesktopSearchTextField(JIPipeRunnableExecutor queue) {
        this.queue = queue;
        this.attentionAnimationTimer = new Timer(ANIMATION_DELAY, e -> updateAttentionAnimation());
        this.attentionAnimationTimer.setRepeats(true);
        this.attentionAnimationTimer.setCoalesce(false);
        initialize();
        updateIcon();

        if (queue != null) {
            queue.getStartedEventEmitter().subscribe(this);
            queue.getFinishedEventEmitter().subscribe(this);
            queue.getInterruptedEventEmitter().subscribe(this);
        }
    }

    private void updateIcon() {
        if (queue == null || queue.isEmpty()) {
            busyIcon.stop();
            searchButton.setIcon(readyIcon);
        } else {
            busyIcon.start();
            searchButton.setIcon(busyIcon);
        }
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

        searchButton.addActionListener(e -> {
            isProgrammaticFocusChange = true;
            try {
                textField.requestFocusInWindow();
                textField.selectAll();
            } finally {
                isProgrammaticFocusChange = false;
            }
        });
        UIUtils.makeButtonFlat25x25(searchButton);
        searchButton.setRequestFocusEnabled(false);
        searchButton.setFocusable(false);

        add(searchButton, BorderLayout.WEST);

        textField.setBorder(null);
        add(textField, BorderLayout.CENTER);

        buttonPanel.setBackground(UIManager.getColor("TextField.background"));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        add(buttonPanel, BorderLayout.EAST);
        addButton("Clear search", JIPipe.RESOURCES.getIcon16("actions/edit-clear.png"), (searchTextField) -> {
            clear();
        });
    }

    public void clear() {
        isProgrammaticFocusChange = true;
        try {
            setText("");
            textField.requestFocusInWindow();
        } finally {
            isProgrammaticFocusChange = false;
        }
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
        searchDebouncer = new StaticDebouncer(SEARCH_DEBOUNCE_DELAY, () ->
                listener.actionPerformed(new ActionEvent(this, 1, "search-text-changed")));

        textField.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                updateSearchStrings();
                searchDebouncer.debounce();
            }
        });

        // Add focus listener to prevent selection conflicts
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (isProgrammaticFocusChange) {
                    // Only select all when it's a programmatic focus change
                    SwingUtilities.invokeLater(() -> {
                        if (textField.isFocusOwner()) {
                            textField.selectAll();
                        }
                    });
                }
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
        if (s == null) {
            s = "";
        }
        for (String searchString : getSearchStrings()) {
            if (!s.toLowerCase(Locale.ROOT).contains(searchString.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    public void grabAttentionAnimation() {
        isProgrammaticFocusChange = true;
        try {
            attentionAnimationStatus = 0;
            attentionAnimationTimer.restart();
            textField.requestFocusInWindow();
        } finally {
            isProgrammaticFocusChange = false;
        }
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        SwingUtilities.invokeLater(this::updateIcon);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        SwingUtilities.invokeLater(this::updateIcon);
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        SwingUtilities.invokeLater(this::updateIcon);
    }
}
