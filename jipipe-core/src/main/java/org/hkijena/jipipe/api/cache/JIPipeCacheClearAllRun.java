package org.hkijena.jipipe.api.cache;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;

public class JIPipeCacheClearAllRun extends AbstractJIPipeRunnable {

    private final JIPipeCache cache;

    public JIPipeCacheClearAllRun(JIPipeCache cache) {
        this.cache = cache;
    }

    @Override
    public String getTaskLabel() {
        return "Clear cache";
    }

    @Override
    public void run() {
        cache.clearAll(getProgressInfo());
        getProgressInfo().log(cache.toString());
    }

    public JIPipeCache getCache() {
        return cache;
    }
}
