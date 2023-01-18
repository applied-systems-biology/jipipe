package org.hkijena.jipipe.api.cache;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;

public class JIPipeCacheClearOutdatedRun extends AbstractJIPipeRunnable {

    private final JIPipeCache cache;

    public JIPipeCacheClearOutdatedRun(JIPipeCache cache) {
        this.cache = cache;
    }

    @Override
    public String getTaskLabel() {
        return "Clear outdated cache items";
    }

    @Override
    public void run() {
        cache.clearOutdated(getProgressInfo());
        getProgressInfo().log(cache.toString());
    }

    public JIPipeCache getCache() {
        return cache;
    }
}
