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

package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.util.Progress;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * Adapter between JIPipe and ImageJ progress implementations
 */
public class ProgressAdapter implements Progress {

    private final JIPipeProgressInfo onProgress;
    private int progress;
    private int maxProgress;
    private String title;
    private String item;

    public ProgressAdapter(JIPipeProgressInfo onProgress) {
        this.onProgress = onProgress;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void setCount(int count, int total) {
        this.progress = count;
        this.maxProgress = total;
    }

    @Override
    public void addItem(Object item) {
        this.item = "" + item;
        postProgress();
    }

    @Override
    public void setItemCount(int count, int total) {
        this.progress = count;
        this.maxProgress = total;
    }

    @Override
    public void itemDone(Object item) {
        this.item = "Done: " + item;
        postProgress();
    }

    @Override
    public void done() {
        this.item = "Done.";
        postProgress();
    }

    private void postProgress() {
        onProgress.setProgress(progress, maxProgress);
        onProgress.log(title + " | " + item);
    }
}
