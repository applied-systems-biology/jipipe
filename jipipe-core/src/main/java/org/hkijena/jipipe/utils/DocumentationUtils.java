package org.hkijena.jipipe.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentationDescription;

import java.io.IOException;
import java.net.URL;

public class DocumentationUtils {

    public static JIPipeDocumentation createDocumentation(String name, String description) {
        return new JIPipeDefaultDocumentation(name, description);
    }

    public static JIPipeDocumentation createDocumentation(String name, String descriptionResourceURL, Class<?> descriptionResourceClass) {
        return new JIPipeDefaultDocumentation(name, descriptionResourceURL, descriptionResourceClass);
    }

    /**
     * Returns the documentation description string if available.
     * Prefers to use the resource URL if set up.
     *
     * @param documentation the documentation annotation
     * @return the description
     */
    public static String getDocumentationDescription(JIPipeDocumentation documentation) {
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
    public static String getDocumentationDescription(JIPipeDocumentationDescription documentation) {
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
     * @param klass the class the contains {@link JIPipeDocumentation} and {@link org.hkijena.jipipe.api.JIPipeDocumentationDescription} items
     * @return the description
     */
    public static String getDocumentationDescription(JIPipeDocumentation documentation, Class<?> klass) {
        StringBuilder builder = new StringBuilder();
        JIPipeDocumentation annotation = klass.getAnnotation(JIPipeDocumentation.class);
        builder.append(getDocumentationDescription(documentation));
        if(annotation != null) {
            String secondary = getDocumentationDescription(annotation);
            if(builder.length() > 0 && !StringUtils.isNullOrEmpty(secondary))
                builder.append("\n\n");
            builder.append(getDocumentationDescription(annotation));
        }
        for (JIPipeDocumentationDescription description : klass.getAnnotationsByType(JIPipeDocumentationDescription.class)) {
            builder.append("\n\n");
            builder.append(getDocumentationDescription(description));
        }
        return builder.toString();
    }

    /**
     * Returns the documentation description string if available.
     * Prefers to use the resource URL if set up.
     *
     * @param klass the class the contains {@link JIPipeDocumentation} and {@link org.hkijena.jipipe.api.JIPipeDocumentationDescription} items
     * @return the description
     */
    public static String getDocumentationDescription(Class<?> klass) {
        StringBuilder builder = new StringBuilder();
        JIPipeDocumentation annotation = klass.getAnnotation(JIPipeDocumentation.class);
        if(annotation != null) {
            builder.append(getDocumentationDescription(annotation));
        }
        for (JIPipeDocumentationDescription description : klass.getAnnotationsByType(JIPipeDocumentationDescription.class)) {
            builder.append("\n\n");
            builder.append(getDocumentationDescription(description));
        }
        return builder.toString();
    }

}
