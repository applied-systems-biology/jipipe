package org.hkijena.jipipe.extensions.python;

/**
 * The supported Python environment types
 */
public enum PythonEnvironmentType {
    System,
    VirtualEnvironment,
    Conda;


    @Override
    public String toString() {
        if (this == VirtualEnvironment)
            return "Virtual environment";
        else
            return this.name();
    }
}
