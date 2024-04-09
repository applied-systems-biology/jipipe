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

package org.hkijena.jipipe.api.backups;

import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of backup items that are related to each other
 */
public class JIPipeProjectBackupItemCollection {

    private String sessionId;
    private boolean legacy;
    private List<JIPipeProjectBackupItem> backupItemList = new ArrayList<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public List<JIPipeProjectBackupItem> getBackupItemList() {
        return backupItemList;
    }

    public void setBackupItemList(List<JIPipeProjectBackupItem> backupItemList) {
        this.backupItemList = backupItemList;
    }

    public String getOriginalProjectPath() {
        for (JIPipeProjectBackupItem backupItem : backupItemList) {
            if (!StringUtils.isNullOrEmpty(backupItem.getOriginalProjectPath())) {
                return backupItem.getOriginalProjectPath();
            }
        }
        return null;
    }

    public String renderName() {
        String originalPath = getOriginalProjectPath();
        if (!StringUtils.isNullOrEmpty(originalPath)) {
            return originalPath + " [" + getSessionId() + "]";
        } else {
            return getSessionId();
        }
    }
}
