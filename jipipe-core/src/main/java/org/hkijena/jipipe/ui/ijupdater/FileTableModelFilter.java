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

package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.ui.swing.updater.ViewOptions;
import net.imagej.updater.FileObject;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.components.SearchTextFieldTableRowFilter;

import javax.swing.table.TableModel;

public class FileTableModelFilter extends SearchTextFieldTableRowFilter {

    private final ViewOptions.Option option;

    /**
     * @param searchTextField the search field
     * @param option          the view option to filter
     */
    public FileTableModelFilter(SearchTextField searchTextField, ViewOptions.Option option) {
        super(searchTextField);
        this.option = option;
    }

    @Override
    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {

        FileTableModel model = (FileTableModel) entry.getModel();
        if (model == null)
            return false;
        if (model.rowToFile == null)
            return false;
        int row = entry.getIdentifier();
        FileObject fileObject = model.rowToFile.get(row);

        if (option != ViewOptions.Option.ALL) {
            boolean success = false;
            FileObject.Status status = fileObject.getStatus();
            FileObject.Action action = fileObject.getAction();
            switch (option) {
                case INSTALLED:
                    success = !(status == FileObject.Status.LOCAL_ONLY || status == FileObject.Status.NOT_INSTALLED);
                    break;
                case UNINSTALLED:
                    success = status == FileObject.Status.NOT_INSTALLED;
                    break;
                case UPTODATE:
                    success = action == FileObject.Action.INSTALLED;
                    break;
                case UPDATEABLE:
                    success = (status == FileObject.Status.UPDATEABLE || status == FileObject.Status.NEW
                            || status == FileObject.Status.OBSOLETE || status == FileObject.Status.OBSOLETE_MODIFIED) ||
                            (action == FileObject.Action.INSTALL || action == FileObject.Action.UPDATE || action == FileObject.Action.UNINSTALL);
                    break;
                case LOCALLY_MODIFIED:
                    success = (status == FileObject.Status.MODIFIED || status == FileObject.Status.OBSOLETE_MODIFIED);
                    break;
                case MANAGED:
                    success = status != FileObject.Status.LOCAL_ONLY;
                    break;
                case OTHERS:
                    success = status == FileObject.Status.LOCAL_ONLY;
                    break;
                case CHANGES:
                    success = action != fileObject.getStatus().getNoAction();
                    break;
            }
            if (!success)
                return false;
        }

        return super.include(entry);
    }
}
