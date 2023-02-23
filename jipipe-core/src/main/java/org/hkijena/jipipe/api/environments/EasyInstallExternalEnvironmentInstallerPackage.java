package org.hkijena.jipipe.api.environments;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.SystemUtils;

import java.util.List;

public class EasyInstallExternalEnvironmentInstallerPackage {
    private String name;
    private String installDir;
    private String description;
    private boolean supportsWindows;
    private boolean supportsLinux;
    private boolean supportsMacOS;
    private JsonNode additionalData;

    private String url;

    private List<String> urlMultiPart;

    private String multiPartOutputName;

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
        return getName() + " [" + getUrl() + "] -> " + getInstallDir() + " on " + "win=" + isSupportsWindows() + ",mac=" + isSupportsMacOS() + ",linux=" + isSupportsMacOS();
    }
}
