package org.hkijena.jipipe.extensions.parameters.library.references;

import org.hkijena.jipipe.api.data.JIPipeData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ImageJImporterParameterSettings {
    /**
     * The data type that is targeted
     * @return the base class
     */
    Class<? extends JIPipeData> baseClass() default JIPipeData.class;

    /**
     * If true, conversions are considered
     * @return if convertible importers are considered
     */
    boolean includeConvertible() default true;
}
