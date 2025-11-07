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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io.backends;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io.BioFormatsImporterAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;

import java.nio.file.Path;

public class BioFormatsImageImageBackend extends PathMatchedImportImageBackend {

    public BioFormatsImageImageBackend() {
    }

    public BioFormatsImageImageBackend(BioFormatsImageImageBackend other) {
        super(other);
    }

    @Override
    public String getDefaultPathMatching() {
        return "true";
    }

    @Override
    public ImagePlus doImport(Path path, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Using BioFormats importer. Please use the Bio-Formats importer node for more settings.");
        BioFormatsImporterAlgorithm importer = JIPipe.createNode(BioFormatsImporterAlgorithm.class);
        importer.getFirstInputSlot().addData(new FileData(path), progressInfo);
        importer.run(runContext, progressInfo);
        return importer.getFirstOutputSlot().getData(0, OMEImageData.class, progressInfo).getImage();
    }
}
