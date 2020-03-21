package org.hkijena.acaq5.api.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Compound type that contains all metadata of an {@link ACAQData} type
 */
public class ACAQDataDeclaration implements Comparable<ACAQDataDeclaration> {
    private static Map<Class<? extends ACAQData>, ACAQDataDeclaration> cache = new HashMap<>();

    private Class<? extends ACAQData> dataClass;
    private String name;
    private String description;
    private String menuPath;
    private boolean hidden;

    private ACAQDataDeclaration(Class<? extends ACAQData> dataClass) {
        this.dataClass = dataClass;
        this.name = ACAQData.getNameOf(dataClass);
        this.description = ACAQData.getDescriptionOf(dataClass);
        this.menuPath = ACAQData.getMenuPathOf(dataClass);
        this.hidden = ACAQData.isHidden(dataClass);
    }

    public Class<? extends ACAQData> getDataClass() {
        return dataClass;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMenuPath() {
        return menuPath;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Override
    public int compareTo(ACAQDataDeclaration o) {
        return name.compareTo(o.name);
    }

    public static ACAQDataDeclaration getInstance(Class<? extends ACAQData> klass) {
        ACAQDataDeclaration declaration = cache.getOrDefault(klass, null);
        if (declaration == null) {
            declaration = new ACAQDataDeclaration(klass);
            cache.put(klass, declaration);
        }
        return declaration;
    }
}
