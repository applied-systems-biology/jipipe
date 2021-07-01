/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.settings;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.parameters.primitives.PathList;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CleanBackupsRun implements JIPipeRunnable {
    private JIPipeWorkbench workbench;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public CleanBackupsRun(JIPipeWorkbench workbench) {
        this.workbench = workbench;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Remove duplicate backups";
    }

    @Override
    public void run() {
        Set<Path> toDelete = new HashSet<>();
        Multimap<String, Path> hashes = HashMultimap.create();

        AutoSaveSettings settings = AutoSaveSettings.getInstance();
        boolean isEnabledBackup = settings.isEnableAutoSave();

        try {
            SwingUtilities.invokeAndWait(() -> settings.setEnableAutoSave(false));
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        try {
            PathList lastSaves = settings.getLastSaves();
            getProgressInfo().setProgress(0, lastSaves.size());

            for (Path lastSave : lastSaves) {
                if (getProgressInfo().isCancelled().get())
                    return;
                JIPipeProgressInfo hashProgress = getProgressInfo().resolveAndLog("Generate SHA1 hash for " + lastSave);
                if (!Files.exists(lastSave)) {
                    hashProgress.log("File is missing");
                    toDelete.add(lastSave);
                }
                try {
                    String sha1 = PathUtils.computeFileSHA1(lastSave.toFile());
                    hashes.put(sha1, lastSave);
                } catch (IOException e) {
                    hashProgress.log("Unable to generate hash");
                    hashProgress.log(e.toString());
                }
                getProgressInfo().incrementProgress();
            }

            if (getProgressInfo().isCancelled().get())
                return;

            for (String sha1 : hashes.keySet()) {
                Collection<Path> paths = hashes.get(sha1);
                if (paths.size() > 1) {
                    progressInfo.log("Hash " + sha1 + " matches following paths:");
                    for (Path path : paths) {
                        progressInfo.log(path.toString());
                    }
                    Iterator<Path> iterator = paths.iterator();
                    iterator.next();
                    while (iterator.hasNext()) {
                        Path path = iterator.next();
                        toDelete.add(path);
                        progressInfo.log("Will delete duplicate: " + path);
                    }
                }
            }

            if (getProgressInfo().isCancelled().get())
                return;

            for (Path path : toDelete) {
                if (getProgressInfo().isCancelled().get())
                    return;
                progressInfo.log("Deleting: " + path);
                try {
                    if (Files.exists(path))
                        Files.delete(path);
                    lastSaves.remove(path);
                } catch (IOException e) {
                    progressInfo.log("Unable to delete: " + e);
                }
            }

            settings.triggerParameterChange("last-saves");
            if (workbench != null) {
                workbench.sendStatusBarText("Deleted " + toDelete.size() + " duplicate/missing backups");
            }
        }
        finally {
            try {
                SwingUtilities.invokeAndWait(() -> settings.setEnableAutoSave(isEnabledBackup));
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
