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

package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.apache.commons.lang3.StringUtils;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.WebUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Download files", description = "Downloads one or multiple files from web resources. This node will download the files and places each one of them into a temporary folder. The output of this node is the path to the downloaded file.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = FileData.class, slotName = "Output", create = true)
public class DownloadFilesDataSource extends JIPipeSimpleIteratingAlgorithm {

    private StringList urls = new StringList();

    public DownloadFilesDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public DownloadFilesDataSource(DownloadFilesDataSource other) {
        super(other);
        this.urls = new StringList(other.urls);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (String urlString : urls) {
            try {
                if (urlString.isEmpty()) {
                    throw new JIPipeValidationRuntimeException(new NullPointerException(),
                            "Invalid URL!",
                            "You provided a URL '" + urlString + "', but it is invalid.",
                            "Please fix the URL.");
                }
                URL url = new URL(urlString);
                String s = StringUtils.stripEnd(urlString, " /\\");
                String[] components = s.split("/");
                String fileName = components[components.length - 1];
                Path targetFile = getNewScratch().resolve(fileName);
                WebUtils.download(url, targetFile, getDisplayName(), progressInfo);

                iterationStep.addOutputData(getFirstOutputSlot(), new FileData(targetFile), progressInfo);
            } catch (MalformedURLException e) {
                throw new JIPipeValidationRuntimeException(e,
                        "Invalid URL!",
                        "You provided a URL '" + urlString + "', but it is invalid.",
                        "Please fix the URL.");
            }
        }
    }

    @JIPipeParameter("urls")
    @SetJIPipeDocumentation(name = "URLs", description = "List of URLs to download.")
    @StringParameterSettings(monospace = true, prompt = "https://...", icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/cloud-download.png")
    public StringList getUrls() {
        return urls;
    }

    @JIPipeParameter("urls")
    public void setUrls(StringList urls) {
        this.urls = urls;
    }
}
