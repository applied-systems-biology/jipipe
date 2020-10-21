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

package org.hkijena.jipipe.extensions.omero.algorithms;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.legacy.convert.DatasetToImagePlusConverter;
import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import omero.ServerError;
import omero.client;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.omero.OMEROCredentials;
import org.hkijena.jipipe.extensions.omero.datatypes.OMEROImageReferenceData;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Import from OMERO", description = "Imports an image from OMERO into ImageJ")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = OMEROImageReferenceData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class ImportOMEROImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OMEROCredentials credentials = new OMEROCredentials();
    private OMEROSession session;

    public ImportOMEROImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(credentials);
    }

    public ImportOMEROImageAlgorithm(ImportOMEROImageAlgorithm other) {
        super(other);
        this.credentials = new OMEROCredentials(other.credentials);
        registerSubParameter(credentials);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        OMEROService service = JIPipe.getInstance().getContext().getService(OMEROService.class);
        try(OMEROSession session = service.createSession(credentials.getLocation())){
            this.session = session;
            super.run(subProgress, algorithmProgress, isCancelled);
        }
        finally {
            this.session = null;
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        OMEROImageReferenceData imageReferenceData = dataBatch.getInputData(getFirstInputSlot(), OMEROImageReferenceData.class);
        client client = session.getClient();
        OMEROService service = JIPipe.getInstance().getContext().getService(OMEROService.class);
        DatasetToImagePlusConverter converter = new DatasetToImagePlusConverter();
        try {
            Dataset dataset = service.downloadImage(client, imageReferenceData.getImageId());
            ImagePlus img = converter.convert(dataset, ImagePlus.class);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
        } catch (ServerError | IOException e) {
            throw new UserFriendlyRuntimeException(e,
                    "Could not download image from OMERO!",
                    getName(),
                    "Tried to download image with ID " + imageReferenceData.getImageId() + " from OMERO, but it failed!",
                    "Please check if the image exists and you have access to it.");
        }
    }

    @JIPipeDocumentation(name = "OMERO Server credentials", description = "The following credentials will be used to connect to the OMERO server. If you leave items empty, they will be " +
            "loaded from the OMERO category at the JIPipe application settings.")
    @JIPipeParameter("credentials")
    public OMEROCredentials getCredentials() {
        return credentials;
    }
}
