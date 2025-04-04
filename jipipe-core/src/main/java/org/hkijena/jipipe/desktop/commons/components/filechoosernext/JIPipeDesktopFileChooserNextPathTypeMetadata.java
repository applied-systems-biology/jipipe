package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.hkijena.jipipe.utils.PathType;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public class JIPipeDesktopFileChooserNextPathTypeMetadata implements Predicate<Path> {
    private final String name;
    private final PathType pathType;
    private final Set<String> extensions;
    private final Icon icon;

    public JIPipeDesktopFileChooserNextPathTypeMetadata(String name, PathType pathType, Set<String> extensions, Icon icon) {
        this.name = name;
        this.pathType = pathType;
        this.extensions = extensions;
        this.icon = icon;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public Icon getIcon() {
        return icon;
    }

    public PathType getPathType() {
        return pathType;
    }

    @Override
    public boolean test(Path path) {
        if (!Files.isDirectory(path) && pathType == PathType.DirectoriesOnly) {
            return false;
        }
        for (String extension : extensions) {
            if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }
}
