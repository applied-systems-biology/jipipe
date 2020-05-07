package org.hkijena.acaq5.extensions.parametereditors.editors;

import org.hkijena.acaq5.api.data.ACAQData;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Settings for {@link ACAQData} parameters
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ACAQDataParameterSettings {
    /**
     * Control which data types are available
     *
     * @return the data base class
     */
    Class<? extends ACAQData> dataBaseClass() default ACAQData.class;

    /**
     * If true, users can pick hidden data types
     *
     * @return if users can also pick hidden data types
     */
    boolean showHidden() default false;
}
