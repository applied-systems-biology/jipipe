package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.Conflicts;
import net.imagej.updater.FilesCollection;

import java.awt.*;
import java.util.ArrayList;

public class ResolveDependencies extends ConflictDialog {

    protected Conflicts conflicts;
    protected boolean forUpload;

    public ResolveDependencies(final Window owner,
                               final FilesCollection files)
    {
        this(owner, files, false);
    }

    public ResolveDependencies(final Window owner,
                               final FilesCollection files, final boolean forUpload)
    {
        super(owner, "Resolve dependencies");

        this.forUpload = forUpload;
        conflicts = new Conflicts(files);
        conflictList = new ArrayList<>();
    }

    @Override
    protected void updateConflictList() {
        conflictList.clear();
        for (final Conflicts.Conflict conflict : conflicts.getConflicts(forUpload))
            conflictList.add(conflict);
    }
}
