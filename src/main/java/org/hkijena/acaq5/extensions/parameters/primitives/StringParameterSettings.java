package org.hkijena.acaq5.extensions.parameters.primitives;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Settings for {@link String} parameters
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StringParameterSettings {
    /**
     * If true, the editor allows to create multiple lines
     *
     * @return if the editor allows to create multiple lines
     */
    boolean multiline() default false;

    /**
     * If true, the text is rendered with monospaced font
     *
     * @return if the text is rendered with monospaced font
     */
    boolean monospace() default false;

    /**
     * @return Icon shown next to the text field (single line only)
     */
    String icon() default "";

    /**
     * @return Prompt shown on the text field
     */
    String prompt() default "";
}
