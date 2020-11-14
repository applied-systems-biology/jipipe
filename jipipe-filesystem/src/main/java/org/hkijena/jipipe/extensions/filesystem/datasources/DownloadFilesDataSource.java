package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.apache.commons.lang.StringUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.WebUtils;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;

@JIPipeDocumentation(name = "Download files", description = "Downloads one or multiple files from web resources. This node will download the files and places each one of them into a temporary folder. The output of this node is the path to the downloaded file.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = FileData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        for (String urlString : urls) {
            try {
                if (urlString.isEmpty()) {
                    throw new UserFriendlyRuntimeException(new NullPointerException(), "Invalid URL!", "Algoritmh '" + getName() + "'", "You provided a URL '" + urlString + "', but it is invalid.", "Please fix the URL.");
                }
                URL url = new URL(urlString);
                String s = StringUtils.stripEnd(urlString, " /\\");
                String[] components = s.split("/");
                String fileName = components[components.length - 1];
                Path targetFile = RuntimeSettings.generateTempDirectory("Download").resolve(fileName);
                DecimalFormat df = new DecimalFormat("#.##");
                df.setRoundingMode(RoundingMode.CEILING);
                try {
                    String[] lastMessage = new String[]{""};
                    WebUtils.download(url, targetFile, total -> {
                        String message = "Downloaded " + df.format(total / 1024.0 / 1024.0) + " MB";
                        if (!Objects.equals(message, lastMessage[0])) {
                            progress.log(message);
                            lastMessage[0] = message;
                        }
                    });
                } catch (IOException e) {
                    throw new UserFriendlyRuntimeException(e, "Error while downloading!", "Algorithm '" + getName() + "'", "There was an error downloading URL '" + url + "' to " + targetFile, "Please check if the URL is valid, an internet connection is available, and the target device has enough space.");
                }

                dataBatch.addOutputData(getFirstOutputSlot(), new FileData(targetFile));
            } catch (MalformedURLException e) {
                throw new UserFriendlyRuntimeException(e, "Invalid URL!", "Algorithm '" + getName() + "'", "You provided a URL '" + urlString + "', but it is invalid.", "Please fix the URL.");
            }
        }
    }

    @JIPipeParameter("urls")
    @JIPipeDocumentation(name = "URLs", description = "List of URLs to download.")
    @StringParameterSettings(monospace = true, prompt = "https://...", icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/cloud-download.png")
    public StringList getUrls() {
        return urls;
    }

    @JIPipeParameter("urls")
    public void setUrls(StringList urls) {
        this.urls = urls;
    }
}
