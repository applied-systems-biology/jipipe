package org.hkijena.jipipe.extensions.parameters.ranges;

import java.awt.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NumberRangeParameterSettings {
    double min();
    double max();
    Class<? extends Supplier<Paint>> trackBackground() default DefaultTrackBackground.class;
}
