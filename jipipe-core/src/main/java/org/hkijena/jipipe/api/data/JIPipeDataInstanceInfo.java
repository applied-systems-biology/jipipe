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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Contains textual information about the data.
 * Supports multiple levels of detail.
 */
public class JIPipeDataInstanceInfo {

    private final List<Entry> entries = new ArrayList<>();

    public JIPipeDataInstanceInfo(Entry... entries) {
        this.entries.addAll(Arrays.asList(entries));
    }

    public JIPipeDataInstanceInfo(List<Entry> entries) {
        this.entries.addAll(entries);
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String toPlainText(boolean detailed, boolean withNames) {
        return toPlainText(detailed ? DetailLevel.Detailed : DetailLevel.Brief, withNames);
    }

    public String toPlainText(DetailLevel detailLevel, boolean withNames) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Entry entry : entries) {
            if(entry.isVisibleIn(detailLevel)) {
                if(!first) {
                    sb.append("\n");
                }
                if(withNames && !StringUtils.isNullOrEmpty(entry.getName())) {
                    sb.append(entry.getName());
                    sb.append(": ");
                }
                sb.append(entry.getValue());
                first = false;
            }
        }
        return  sb.toString();
    }

    public HTMLText toHtml(boolean detailed, boolean withNames) {
        return toHtml(detailed ? DetailLevel.Detailed : DetailLevel.Brief, withNames);
    }

    public HTMLText toHtml(DetailLevel detailLevel, boolean withNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        boolean first = true;
        for (Entry entry : entries) {
            if(entry.isVisibleIn(detailLevel)) {
                if(!first) {
                    sb.append("<br/>");
                }
                if(withNames && !StringUtils.isNullOrEmpty(entry.getName())) {
                    sb.append(entry.getName());
                    sb.append(": ");
                }
                sb.append(entry.getValue());
                first = false;
            }
        }
        sb.append("</html>");
        return  new HTMLText(sb.toString());
    }

    public static class Entry {
        private final DetailLevel detailLevel;
        private final String name;
        private final String value;

        public Entry(DetailLevel detailLevel, String name, String value) {
            this.detailLevel = detailLevel;
            this.name = name;
            this.value = value;
        }

        public DetailLevel getDetailLevel() {
            return detailLevel;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public boolean isVisibleIn(DetailLevel detailLevel) {
            if(detailLevel == DetailLevel.Verbose) {
                return true;
            }
            else if(detailLevel == DetailLevel.Detailed) {
                return this.detailLevel != DetailLevel.Verbose;
            }
            else {
                return this.detailLevel == DetailLevel.Brief;
            }
        }
    }

    public enum DetailLevel {
        Brief,
        Detailed,
        Verbose
    }

    public static class Builder {
        private final List<Entry> entries = new ArrayList<>();
        public Builder add(DetailLevel detailLevel, String name, String value) {
            entries.add(new Entry(detailLevel, name, value));
            return this;
        }

        public Builder add(DetailLevel detailLevel, String value) {
            entries.add(new Entry(detailLevel, "", value));
            return this;
        }

        public JIPipeDataInstanceInfo build() {
            return new JIPipeDataInstanceInfo(entries);
        }
    }

}
