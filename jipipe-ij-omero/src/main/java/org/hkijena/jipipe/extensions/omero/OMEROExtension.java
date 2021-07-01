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

package org.hkijena.jipipe.extensions.omero;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.omero.algorithms.DownloadOMEROImageAlgorithm;
import org.hkijena.jipipe.extensions.omero.algorithms.DownloadOMEROTableAlgorithm;
import org.hkijena.jipipe.extensions.omero.algorithms.UploadOMEROImageAlgorithm;
import org.hkijena.jipipe.extensions.omero.algorithms.UploadOMEROTableAlgorithm;
import org.hkijena.jipipe.extensions.omero.datasources.OMERODatasetReferenceDataSource;
import org.hkijena.jipipe.extensions.omero.datasources.OMEROFindDatasetAlgorithm;
import org.hkijena.jipipe.extensions.omero.datasources.OMEROFindImageAlgorithm;
import org.hkijena.jipipe.extensions.omero.datasources.OMEROFindProjectAlgorithm;
import org.hkijena.jipipe.extensions.omero.datasources.OMEROImageReferenceDataSource;
import org.hkijena.jipipe.extensions.omero.datasources.OMEROProjectReferenceDataSource;
import org.hkijena.jipipe.extensions.omero.datatypes.OMERODatasetReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROProjectReferenceData;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Provides data types dor handling strings
 */
@Plugin(type = JIPipeJavaExtension.class)
public class OMEROExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "OMERO Integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates OMERO");
    }

    @Override
    public void register() {
        registerSettingsSheet(OMEROSettings.ID, "OMERO", UIUtils.getIconFromResources("apps/omero.png"), "Extensions", null, new OMEROSettings());
        registerDatatype("omero-project-id", OMEROProjectReferenceData.class, UIUtils.getIconURLFromResources("apps/omero.png"), null, null);
        registerDatatype("omero-dataset-id", OMERODatasetReferenceData.class, UIUtils.getIconURLFromResources("apps/omero.png"), null, null);
        registerDatatype("omero-image-id", OMEROImageReferenceData.class, UIUtils.getIconURLFromResources("apps/omero.png"), null, null);

        registerNodeType("omero-image-id-definition", OMEROImageReferenceDataSource.class);
        registerNodeType("omero-dataset-id-definition", OMERODatasetReferenceDataSource.class);
        registerNodeType("omero-project-id-definition", OMEROProjectReferenceDataSource.class);
        registerNodeType("omero-find-project-id", OMEROFindProjectAlgorithm.class);
        registerNodeType("omero-find-dataset-id", OMEROFindDatasetAlgorithm.class);
        registerNodeType("omero-find-image-id", OMEROFindImageAlgorithm.class);
        registerNodeType("omero-download-image", DownloadOMEROImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/cloud-download.png"));
        registerNodeType("omero-upload-image", UploadOMEROImageAlgorithm.class, UIUtils.getIconURLFromResources("actions/cloud-upload.png"));
        registerNodeType("omero-download-table", DownloadOMEROTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/cloud-download.png"));
        registerNodeType("omero-upload-table", UploadOMEROTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/cloud-upload.png"));
    }

//    @Override
//    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
//        return Collections.singletonList(
//            new JIPipeImageJUpdateSiteDependency(new UpdateSite("OMERO 5.4", "https://sites.imagej.net/OMERO-5.4/", "", "", "", "", 0))
//        );
//    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/omero.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:omero";
    }

    @Override
    public String getDependencyVersion() {
        return "2021.7";
    }
}
