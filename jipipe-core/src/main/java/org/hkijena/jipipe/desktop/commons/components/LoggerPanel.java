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
    
    // Performance optimization fields
    private int currentLineCount = 0;
    private int cachedDocumentLength = 0;
    private final Object lineCountLock = new Object();

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
     * Gets the current line count efficiently using cached value.
     *
     * @return the current number of lines in the log
     */
    public int getCurrentLineCount() {
        synchronized (lineCountLock) {
            return currentLineCount;
        }
    }

    /**
     * Updates the line count incrementally based on the added text.
     *
     * @param addedText the text that was added
     */
    private void updateLineCount(String addedText) {
        synchronized (lineCountLock) {
            if (addedText != null && !addedText.isEmpty()) {
                // Count newlines in the added text
                int newLines = 0;
                for (int i = 0; i < addedText.length(); i++) {
                    if (addedText.charAt(i) == '\n') {
                        newLines++;
                    }
                }
                // If the text doesn't end with newline, add one more line for the partial line
                if (!addedText.endsWith("\n") && !addedText.isEmpty()) {
                    newLines++;
                }
                currentLineCount += newLines;
            }
        }
    }

    /**
     * Removes old lines efficiently when line limit is exceeded.
     * Removes multiple lines at once for better performance.
     */
    private void removeOldLinesIfNeeded() {
        if (!limitLines || maxLines <= 0) {
            return;
        }

        synchronized (lineCountLock) {
            if (currentLineCount <= maxLines) {
                return;
            }

            // Calculate how many lines to remove (remove 20% at once, but at least 1 line)
            int linesToRemove = Math.max(1, (int) (currentLineCount * 0.2));
            linesToRemove = Math.min(linesToRemove, currentLineCount - maxLines);

            // Find the position where the remaining lines start
            int targetLineCount = currentLineCount - linesToRemove;
            int startPos = findLineStartPosition(targetLineCount);

            if (startPos > 0) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        textArea.getDocument().remove(0, startPos);
                        // Update line count after removal
                        currentLineCount = targetLineCount;
                        cachedDocumentLength = textArea.getDocument().getLength();
                    } catch (Exception e) {
                        // Fallback: clear the document if removal fails
                        textArea.setText("");
                        currentLineCount = 0;
                        cachedDocumentLength = 0;
                    }
                });
            }
        }
    }

    /**
     * Finds the starting position of a specific line in the document.
     *
     * @param targetLineCount the line number to find the start position for
     * @return the character position where the line starts, or 0 if not found
     */
    private int findLineStartPosition(int targetLineCount) {
        if (targetLineCount <= 0) {
            return 0;
        }

        try {
            String text = textArea.getText();
            int lineCount = 0;
            int startPos = 0;

            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lineCount++;
                    if (lineCount >= targetLineCount) {
                        return startPos;
                    }
                    startPos = i + 1;
                }
            }

            // Handle the last line if it doesn't end with newline
            if (lineCount + 1 >= targetLineCount) {
                return startPos;
            }

            return 0;
        } catch (Exception e) {
            // Fallback to document length if text extraction fails
            return textArea.getDocument().getLength();
        }
    }

    /**
     * Appends a line to the log and optionally scrolls down.
     * Implements high-performance line limiting when enabled.
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

            // Update document length cache
            int oldLength = cachedDocumentLength;
            textArea.append(lineToAdd);
            cachedDocumentLength = textArea.getDocument().getLength();

            // Update line count incrementally
            updateLineCount(lineToAdd);

            // Remove old lines if limit is exceeded
            removeOldLinesIfNeeded();

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
        SwingUtilities.invokeLater(() -> {
            textArea.setText(str);
            // Reset line count and cache
            synchronized (lineCountLock) {
                currentLineCount = calculateLineCount(str);
                cachedDocumentLength = str.length();
            }
        });
    }

    /**
     * Calculates line count from a string (helper method).
     *
     * @param text the text to count lines in
     * @return the number of lines
     */
    private int calculateLineCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int lineCount = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineCount++;
            }
        }
        // Add one for the last line if it doesn't end with newline
        if (!text.endsWith("\n") && !text.isEmpty()) {
            lineCount++;
        }
        return lineCount;
    }
}
