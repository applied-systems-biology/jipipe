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

package org.hkijena.jipipe.desktop.api.environments;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.WebUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@JsonSerialize(using = JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage.Serializer.class)
public class JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage {
    private String name;
    private String installDir;
    private String description;

    private String version;
    private boolean supportsWindows;
    private boolean supportsLinux;
    private boolean supportsMacOS;
    private JsonNode additionalData;

    private String url;

    private List<String> urlMultiPart;

    private String multiPartOutputName;

    public static List<JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage> loadFromJson(JsonNode rootNode, JIPipeProgressInfo repositoryProgress) {
        List<JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage> availablePackages = new ArrayList<>();
        for (JsonNode packageNodeEntry : ImmutableList.copyOf(rootNode.get("files").elements())) {
            JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage availablePackage = new JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage();
            availablePackage.setName(packageNodeEntry.get("name").textValue());
            availablePackage.setDescription(packageNodeEntry.get("description").textValue());
            availablePackage.setInstallDir(packageNodeEntry.get("install-dir").textValue());
            availablePackage.setVersion(packageNodeEntry.get("version").textValue());

            availablePackage.setAdditionalData(packageNodeEntry);

            // Read URL
            JsonNode urlNode = packageNodeEntry.path("url");
            if (!urlNode.isMissingNode()) {
                availablePackage.setUrl(urlNode.textValue());
            }

            // Multipart URL
            JsonNode urlMultiPartNode = packageNodeEntry.path("url-multipart");
            if (!urlMultiPartNode.isMissingNode()) {
                availablePackage.setUrlMultiPart(new ArrayList<>());
                for (JsonNode node : ImmutableList.copyOf(urlMultiPartNode.elements())) {
                    availablePackage.getUrlMultiPart().add(node.textValue());
                }
                availablePackage.setMultiPartOutputName(packageNodeEntry.get("multipart-output-name").textValue());
            }

            // Read operating system
            JsonNode operatingSystemsNode = packageNodeEntry.path("operating-systems");
            if (operatingSystemsNode.isMissingNode()) {
                availablePackage.setSupportsLinux(true);
                availablePackage.setSupportsMacOS(true);
                availablePackage.setSupportsWindows(true);
            } else {
                Set<String> supported = new HashSet<>();
                for (JsonNode element : ImmutableList.copyOf(operatingSystemsNode.elements())) {
                    supported.add(element.textValue().toLowerCase(Locale.ROOT));
                }
                availablePackage.setSupportsLinux(supported.contains("linux"));
                availablePackage.setSupportsMacOS(supported.contains("macos") || supported.contains("osx"));
                availablePackage.setSupportsWindows(supported.contains("windows") || supported.contains("win"));
            }
            repositoryProgress.log("Detected package " + availablePackage);
            availablePackages.add(availablePackage);
        }
        return availablePackages;
    }

    public static List<JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage> loadFromURLs(List<String> repositories, JIPipeProgressInfo progressInfo) {
        List<JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage> availablePackages = new ArrayList<>();
        progressInfo.log("Following repositories will be contacted:");
        for (int i = 0; i < repositories.size(); i++) {
            String repository = repositories.get(i);
            progressInfo.log(" - [Repository " + i + "] " + repository);
        }
        for (int i = 0; i < repositories.size(); i++) {
            String repository = repositories.get(i);
            JIPipeProgressInfo repositoryProgress = progressInfo.resolve("Repository " + i);
            Path outputFile = JIPipeRuntimeApplicationSettings.getTemporaryFile("repository", ".json");
            try {
                WebUtils.download(new URL(repository), outputFile, "Download repository", repositoryProgress);
            } catch (MalformedURLException e) {
                repositoryProgress.log(e.toString());
                repositoryProgress.log(e.getMessage());
                repositoryProgress.log("-> Skipping repository " + repository + ". Please check the URL!");
            }

            // Import the repository
            JsonNode rootNode = JsonUtils.readFromFile(outputFile, JsonNode.class);
            availablePackages.addAll(JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage.loadFromJson(rootNode, repositoryProgress));

            try {
                Files.delete(outputFile);
            } catch (IOException e) {
                repositoryProgress.log("Could not clean up temporary file " + outputFile);
                repositoryProgress.log(e.toString());
            }
        }

        return availablePackages;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMultiPartOutputName() {
        return multiPartOutputName;
    }

    public void setMultiPartOutputName(String multiPartOutputName) {
        this.multiPartOutputName = multiPartOutputName;
    }

    public List<String> getUrlMultiPart() {
        return urlMultiPart;
    }

    public void setUrlMultiPart(List<String> urlMultiPart) {
        this.urlMultiPart = urlMultiPart;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstallDir() {
        return installDir;
    }

    public void setInstallDir(String installDir) {
        this.installDir = installDir;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSupportsWindows() {
        return supportsWindows;
    }

    public void setSupportsWindows(boolean supportsWindows) {
        this.supportsWindows = supportsWindows;
    }

    public boolean isSupportsLinux() {
        return supportsLinux;
    }

    public void setSupportsLinux(boolean supportsLinux) {
        this.supportsLinux = supportsLinux;
    }

    public boolean isSupportsMacOS() {
        return supportsMacOS;
    }

    public void setSupportsMacOS(boolean supportsMacOS) {
        this.supportsMacOS = supportsMacOS;
    }

    public JsonNode getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(JsonNode additionalData) {
        this.additionalData = additionalData;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isSupported() {
        if (SystemUtils.IS_OS_WINDOWS)
            return supportsWindows;
        else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX)
            return supportsMacOS;
        else if (SystemUtils.IS_OS_LINUX)
            return supportsLinux;
        else
            return false;
    }

    public boolean isUnsupported() {
        return !isSupported();
    }

    @Override
    public String toString() {
        return getName() + " v" + getVersion() + " [" + getUrl() + "] -> " + getInstallDir() + " on " + "win=" + isSupportsWindows() + ",mac=" + isSupportsMacOS() + ",linux=" + isSupportsMacOS();
    }

    public static class Serializer extends JsonSerializer<JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage> {
        @Override
        public void serialize(JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name);
            gen.writeStringField("install-dir", value.installDir);
            gen.writeStringField("version", value.version);
            gen.writeStringField("description", value.description);
            if (!StringUtils.isNullOrEmpty(value.url))
                gen.writeStringField("url", value.url);
            if (value.urlMultiPart != null && !value.getUrlMultiPart().isEmpty()) {
                gen.writeObjectField("url-multipart", value.getUrlMultiPart());
                gen.writeStringField("multipart-output-name", value.multiPartOutputName);
            }
            gen.writeArrayFieldStart("operating-systems");
            if (value.isSupportsWindows())
                gen.writeString("windows");
            if (value.isSupportsLinux())
                gen.writeString("linux");
            if (value.isSupportsMacOS())
                gen.writeString("macos");
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }
}
