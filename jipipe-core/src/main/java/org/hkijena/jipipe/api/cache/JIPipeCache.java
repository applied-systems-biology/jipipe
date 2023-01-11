package org.hkijena.jipipe.api.cache;

import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * A class that implements a data cache
 */
public interface JIPipeCache {
    /**
     * Removes all outdated entries
     * @param progressInfo the progress
     */
    void removeOutdated(JIPipeProgressInfo progressInfo);

    /**
     * Clears the cache
     * @param progressInfo the progress
     */
    void clear(JIPipeProgressInfo progressInfo);
}
