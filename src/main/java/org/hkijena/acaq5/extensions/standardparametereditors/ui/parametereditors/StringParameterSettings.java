package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StringParameterSettings {
    /**
     * If true, the editor allows to create multiple lines
     *
     * @return
     */
    boolean multiline() default false;

    /**
     * If true, the text is rendered with monospaced font
     *
     * @return
     */
    boolean monospace() default false;
}
