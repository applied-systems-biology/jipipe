package org.hkijena.acaq5.api.parameters;

/**
 * A type that describes an ACAQ parameter type
 */
public interface ACAQParameterTypeDeclaration {

    /**
     * Creates a new non-null instance of the parameter type
     *
     * @return the instance
     */
    Object newInstance();

    /**
     * Duplicates the parameter
     *
     * @param original the original
     * @return the copy.
     */
    Object duplicate(Object original);

    /**
     * @return the unique ID of this parameter type
     */
    String getId();

    /**
     * @return the Java class of this parameter type
     */
    Class<?> getFieldClass();

    /**
     * @return a short descriptive name
     */
    String getName();

    /**
     * @return a description
     */
    String getDescription();
}
