package org.hkijena.jipipe.utils;

/**
 * Determines the type of selected path
 */
public enum PathType {
    FilesOnly,
    DirectoriesOnly,
    FilesAndDirectories;


    @Override
    public String toString() {
        switch (this) {
            case FilesOnly:
                return "Only files";
            case DirectoriesOnly:
                return "Only directories";
            case FilesAndDirectories:
                return "Files or directories";
        }
        throw new UnsupportedOperationException();
    }
}
