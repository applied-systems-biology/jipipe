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

package org.hkijena.jipipe.api;

import java.lang.annotation.Annotation;

/**
 * Default implementation of {@link JIPipeDocumentation}
 */
public class JIPipeDefaultDocumentation implements JIPipeDocumentation {
    private final String name;
    private final String description;
    private final String descriptionResourceURL;
    private final Class<?> descriptionResourceClass;


    public JIPipeDefaultDocumentation(String name, String description) {
        this.name = name;
        this.description = description;
        this.descriptionResourceURL = null;
        this.descriptionResourceClass = null;
    }

    public JIPipeDefaultDocumentation(String name, String descriptionResourceURL, Class<?> descriptionResourceClass) {
        this.name = name;
        this.description = null;
        this.descriptionResourceURL = descriptionResourceURL;
        this.descriptionResourceClass = descriptionResourceClass;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public String descriptionResourceURL() {
        return this.descriptionResourceURL;
    }

    @Override
    public Class<?> descriptionResourceClass() {
        return this.descriptionResourceClass;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return JIPipeDocumentation.class;
    }
}
