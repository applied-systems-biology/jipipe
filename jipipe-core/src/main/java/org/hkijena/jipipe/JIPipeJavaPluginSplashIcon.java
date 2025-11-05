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

package org.hkijena.jipipe;

import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;

public class JIPipeJavaPluginSplashIcon {
    private String id;
    private String name;
    private String url;
    private ImageIcon icon;

    public JIPipeJavaPluginSplashIcon() {
    }

    public JIPipeJavaPluginSplashIcon(JIPipeJavaPluginSplashIcon other) {
        this.id = other.id;
        this.name = other.name;
        this.url = other.url;
        this.icon = other.icon;
    }

    public static JIPipeJavaPluginSplashIconBuilder builder() {
        return new JIPipeJavaPluginSplashIconBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

    public static final class JIPipeJavaPluginSplashIconBuilder {
        private final JIPipeJavaPluginSplashIcon result;

        public JIPipeJavaPluginSplashIconBuilder() {
            result = new JIPipeJavaPluginSplashIcon();
        }

        public JIPipeJavaPluginSplashIconBuilder id(String id) {
            result.setId(id);
            return this;
        }

        public JIPipeJavaPluginSplashIconBuilder name(String name) {
            result.setName(name);
            return this;
        }

        public JIPipeJavaPluginSplashIconBuilder url(String url) {
            result.setUrl(url);
            return this;
        }

        public JIPipeJavaPluginSplashIconBuilder icon(ImageIcon icon) {
            result.setIcon(icon);
            return this;
        }

        public JIPipeJavaPluginSplashIcon build() {
            if (StringUtils.isNullOrEmpty(result.getId())) {
                throw new IllegalArgumentException("ID cannot be null or empty");
            }
            return new JIPipeJavaPluginSplashIcon(result);
        }
    }
}
