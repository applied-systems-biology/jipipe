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

package org.hkijena.jipipe.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;

import java.io.IOException;
import java.net.URL;

public class DocumentationUtils {

    public static SetJIPipeDocumentation createDocumentation(String name, String description) {
        return new JIPipeDocumentation(name, description);
    }

    public static SetJIPipeDocumentation createDocumentation(String name, String descriptionResourceURL, Class<?> descriptionResourceClass) {
        return new JIPipeDocumentation(name, descriptionResourceURL, descriptionResourceClass);
    }

    /**
     * Returns the documentation description string if available.
     * Prefers to use the resource URL if set up.
     *
     * @param documentation the documentation annotation
     * @return the description
     */
    public static String getDocumentationDescription(SetJIPipeDocumentation documentation) {
        if (!StringUtils.isNullOrEmpty(documentation.descriptionResourceURL())) {
            URL url = documentation.descriptionResourceClass().getResource(documentation.descriptionResourceURL());
            try {
                return Resources.toString(url, Charsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return documentation.description();
        }
    }

    /**
     * Returns the documentation description string if available.
     * Prefers to use the resource URL if set up.
     *
     * @param documentation the documentation annotation
     * @return the description
     */
    public static String getDocumentationDescription(AddJIPipeDocumentationDescription documentation) {
        if (!StringUtils.isNullOrEmpty(documentation.descriptionResourceURL())) {
            URL url = documentation.descriptionResourceClass().getResource(documentation.descriptionResourceURL());
            try {
                return Resources.toString(url, Charsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return documentation.description();
        }
    }

    /**
     * Returns the documentation description string if available.
     * Prefers to use the resource URL if set up.
     *
     * @param documentation the first documentation
     * @param klass         the class the contains {@link SetJIPipeDocumentation} and {@link AddJIPipeDocumentationDescription} items
     * @return the description
     */
    public static String getDocumentationDescription(SetJIPipeDocumentation documentation, Class<?> klass) {
        StringBuilder builder = new StringBuilder();
        SetJIPipeDocumentation annotation = klass.getAnnotation(SetJIPipeDocumentation.class);
        builder.append(getDocumentationDescription(documentation));
        if (annotation != null) {
            String secondary = getDocumentationDescription(annotation);
            if (builder.length() > 0 && !StringUtils.isNullOrEmpty(secondary))
                builder.append("\n\n");
            builder.append(getDocumentationDescription(annotation));
        }
        for (AddJIPipeDocumentationDescription description : klass.getAnnotationsByType(AddJIPipeDocumentationDescription.class)) {
            builder.append("\n\n");
            builder.append(getDocumentationDescription(description));
        }
        return builder.toString();
    }

    /**
     * Returns the documentation description string if available.
     * Prefers to use the resource URL if set up.
     *
     * @param klass the class the contains {@link SetJIPipeDocumentation} and {@link AddJIPipeDocumentationDescription} items
     * @return the description
     */
    public static String getDocumentationDescription(Class<?> klass) {
        StringBuilder builder = new StringBuilder();
        SetJIPipeDocumentation annotation = klass.getAnnotation(SetJIPipeDocumentation.class);
        if (annotation != null) {
            builder.append(getDocumentationDescription(annotation));
        }
        for (AddJIPipeDocumentationDescription description : klass.getAnnotationsByType(AddJIPipeDocumentationDescription.class)) {
            builder.append("\n\n");
            builder.append(getDocumentationDescription(description));
        }
        return builder.toString();
    }

}
