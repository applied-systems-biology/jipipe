package org.hkijena.jipipe.api;

/**
 * Implement this interface to allow the comparison of function (i.e. two objects can be different, but have the same function and yield the same results)
 * Useful for custom parameter types.
 */
public interface JIPipeFunctionallyComparable {
    boolean functionallyEquals(Object other);
}
