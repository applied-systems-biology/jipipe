package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.util.Progress;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

public class JIPipeProgressAdapter implements Progress {

    private final JIPipeProgressInfo progressInfo;
    private String title;
    private String item;

    public JIPipeProgressAdapter(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void setCount(int count, int total) {
        this.progressInfo.setProgress(count, total);
    }

    @Override
    public void addItem(Object item) {
        this.item = "" + item;
        postProgress();
    }

    @Override
    public void setItemCount(int count, int total) {
        this.progressInfo.setProgress(count, total);
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
        progressInfo.log(title + " " + item);
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }
}
