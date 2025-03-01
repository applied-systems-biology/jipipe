package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileChooserHistory {

    private final List<Path> history = new ArrayList<>();
    private int currentIndex = -1;

    /**
     * Set a new directory, adding it to the history.
     * This should be called whenever the directory is changed.
     */
    public void insert(Path directory) {
        if (directory == null) {
            return;
        }

        // If we are not at the end of history (i.e., user navigated back), we trim forward history
        if (currentIndex < history.size() - 1) {
            history.subList(currentIndex + 1, history.size()).clear();
        }

        // Add to history and move current index
        history.add(directory);
        currentIndex = history.size() - 1;
    }

    /**
     * Moves back in the history.
     * Returns the previous directory or null if no previous directory exists.
     */
    public Path goBack() {
        if (currentIndex > 0) {
            currentIndex--;
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * Moves forward in the history.
     * Returns the next directory or null if no next directory exists.
     */
    public Path goForward() {
        if (currentIndex < history.size() - 1) {
            currentIndex++;
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * Clears the entire history.
     */
    public void clear() {
        history.clear();
        currentIndex = -1;
    }

    /**
     * Get the current directory in history, or null if history is empty.
     */
    public Path getCurrentDirectory() {
        if (currentIndex >= 0 && currentIndex < history.size()) {
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * Checks if back navigation is possible.
     */
    public boolean canGoBack() {
        return currentIndex > 0;
    }

    /**
     * Checks if forward navigation is possible.
     */
    public boolean canGoForward() {
        return currentIndex < history.size() - 1;
    }
}

