package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class LoggerPanel extends JPanel {

    private JScrollPane scrollPane;
    private JTextArea textArea;
    private boolean autoScrollEnabled = true;
    private boolean limitLines = true;
    private int maxLines = 8192;

    public LoggerPanel() {
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(8,8));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        textArea = new  JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall()));

        scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Scrolls to the last line of the log reliably.
     * This method works even after user interactions with the text area or scroll pane.
     */
    public void scrollToLastLine() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Position caret at the end of the document
                textArea.setCaretPosition(textArea.getDocument().getLength());

                // Get the caret visual position
                Point caretPos = textArea.getCaret().getMagicCaretPosition();
                if (caretPos != null) {
                    // Create a small rectangle around the caret position
                    Rectangle visibleRect = new Rectangle(caretPos.x, caretPos.y, 1, 1);
                    textArea.scrollRectToVisible(visibleRect);
                }
            } catch (Exception e) {
                // Fallback to direct scroll bar manipulation
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                verticalBar.setValue(verticalBar.getMaximum());
            }
        });
    }


    /**
     * Appends a line to the log and optionally scrolls down.
     *
     * @param line the line to append
     * @param scrollDown whether to scroll down after appending
     */
    public void appendLine(String line, boolean scrollDown) {
        SwingUtilities.invokeLater(() -> {
            // Append the line with a newline if not already present
            String lineToAdd = line;
            if (!lineToAdd.endsWith("\n")) {
                lineToAdd += "\n";
            }

            textArea.append(lineToAdd);

            if (scrollDown) {
                scrollToLastLine();
            }
        });
    }

    /**
     * Appends a line to the log and scrolls down if auto-scroll is enabled.
     *
     * @param line the line to append
     */
    public void appendLine(String line) {
        appendLine(line, autoScrollEnabled);
    }

    /**
     * Sets whether auto-scroll is enabled.
     *
     * @param enabled true to enable auto-scroll, false otherwise
     */
    public void setAutoScrollEnabled(boolean enabled) {
        this.autoScrollEnabled = enabled;
    }

    /**
     * Returns whether auto-scroll is enabled.
     *
     * @return true if auto-scroll is enabled, false otherwise
     */
    public boolean isAutoScrollEnabled() {
        return autoScrollEnabled;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public boolean isLimitLines() {
        return limitLines;
    }

    public void setLimitLines(boolean limitLines) {
        this.limitLines = limitLines;
    }

    public void setLogText(String str) {
        textArea.setText(str);
    }
}
