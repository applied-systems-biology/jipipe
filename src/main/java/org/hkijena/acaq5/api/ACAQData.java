package org.hkijena.acaq5.api;

public interface ACAQData {

    /**
     * Returns the name of a data type
     * @param klass
     * @return
     */
    static String getNameOf(Class<? extends ACAQData> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if(annotations.length > 0) {
            return annotations[0].name();
        }
        else {
            return klass.getSimpleName();
        }
    }
}
