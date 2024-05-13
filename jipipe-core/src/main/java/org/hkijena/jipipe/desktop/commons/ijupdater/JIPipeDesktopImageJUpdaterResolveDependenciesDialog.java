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

package org.hkijena.jipipe.desktop.commons.ijupdater;

import net.imagej.updater.Conflicts;
import net.imagej.updater.FilesCollection;

import java.awt.*;
import java.util.ArrayList;

public class JIPipeDesktopImageJUpdaterResolveDependenciesDialog extends JIPipeDesktopImageJUpdaterConflictDialog {

    protected Conflicts conflicts;
    protected boolean forUpload;

    public JIPipeDesktopImageJUpdaterResolveDependenciesDialog(final Window owner,
                                                               final FilesCollection files) {
        this(owner, files, false);
    }

    public JIPipeDesktopImageJUpdaterResolveDependenciesDialog(final Window owner,
                                                               final FilesCollection files, final boolean forUpload) {
        super(owner, "Resolve dependencies");

        this.forUpload = forUpload;
        conflicts = new Conflicts(files);
        conflictList = new ArrayList<>();
    }

    @Override
    protected void updateConflictList() {
        conflictList.clear();
        for (final Conflicts.Conflict conflict : conflicts.getConflicts(forUpload)) {
            conflictList.add(conflict);
        }
    }
}
