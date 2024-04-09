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

package org.hkijena.jipipe.plugins.omero.util;

import ome.formats.importer.IObservable;
import ome.formats.importer.IObserver;
import ome.formats.importer.ImportEvent;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

public class OMEROUploadToJIPipeLogger implements IObserver {

    private final JIPipeProgressInfo info;

    public OMEROUploadToJIPipeLogger(JIPipeProgressInfo info) {
        this.info = info;
    }

    @Override
    public void update(IObservable observable, ImportEvent event) {
        info.log(event.toLog());
    }
}
