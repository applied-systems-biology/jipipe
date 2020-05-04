package org.hkijena.acaq5.api.exceptions;

/**
 * Error/Exception that contains user-friendly information
 */
public interface UserFriendlyException {
    String getUserWhat();

    String getUserWhy();

    String getUserHow();

    String getUserWhere();
}
