package org.hkijena.jipipe.extensions.parameters.library.ranges;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NumberRangeParameterSettings {
    double min();

    double max();

    Class<? extends PaintGenerator> trackBackground() default DefaultTrackBackground.class;

    NumberRangeInvertedMode invertedMode() default NumberRangeInvertedMode.SwitchMinMax;
}