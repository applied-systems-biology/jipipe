package org.hkijena.acaq5.extensions;

import org.hkijena.acaq5.ACAQDefaultJavaExtension;
import org.hkijena.acaq5.utils.ResourceUtils;

import java.net.URL;

public abstract class ACAQPrepackagedDefaultJavaExtension extends ACAQDefaultJavaExtension {

    @Override
    public String getAuthors() {
        return "Zoltán Cseresnyés, Ruman Gerst";
    }

    @Override
    public String getWebsite() {
        return "https://applied-systems-biology.github.io/acaq5/";
    }

    @Override
    public String getLicense() {
        return "BSD-2";
    }

    @Override
    public URL getLogo() {
        return ResourceUtils.getPluginResource("logo-400.png");
    }

    @Override
    public String getCitation() {
        return "";
    }
}
