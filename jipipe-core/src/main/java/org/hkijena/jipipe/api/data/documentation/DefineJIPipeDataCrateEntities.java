package org.hkijena.jipipe.api.data.documentation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DefineJIPipeDataCrateEntities {
    DefineJIPipeDataCrateEntity[] value();
}
