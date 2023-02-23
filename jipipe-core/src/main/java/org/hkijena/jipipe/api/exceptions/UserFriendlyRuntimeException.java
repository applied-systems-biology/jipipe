/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.exceptions;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * A {@link RuntimeException} that contains more detailed information for users.
 * It answers in understandable terms: (1) What happened (2) Where it happened (3) Why it happened, and (4) What to do to resolve the issue
 */
public class UserFriendlyRuntimeException extends RuntimeException implements UserFriendlyException {
    private final String userWhat;
    private final String userWhere;
    private final String userWhy;
    private final String userHow;


    /**
     * @param message   the developer message
     * @param cause     the underlying exception
     * @param userWhat  the message for the user
     * @param userWhere location of the exception
     * @param userWhy   explanation why it happened
     * @param userHow   explanation how to resolve the issue
     */
    public UserFriendlyRuntimeException(String message, Throwable cause, String userWhat, String userWhere, String userWhy, String userHow) {
        super(message, cause);
        this.userWhat = userWhat;
        this.userWhere = userWhere;
        this.userWhy = userWhy;
        this.userHow = userHow;
    }

    /**
     * @param message   the developer message
     * @param userWhat  the message for the user
     * @param userWhere location of the exception
     * @param userWhy   explanation why it happened
     * @param userHow   explanation how to resolve the issue
     */
    public UserFriendlyRuntimeException(String message, String userWhat, String userWhere, String userWhy, String userHow) {
        super(message);
        this.userWhat = userWhat;
        this.userWhere = userWhere;
        this.userWhy = userWhy;
        this.userHow = userHow;
    }

    /**
     * @param cause     the underlying exception
     * @param userWhat  the message for the user
     * @param userWhere location of the exception
     * @param userWhy   explanation why it happened
     * @param userHow   explanation how to resolve the issue
     */
    public UserFriendlyRuntimeException(Throwable cause, String userWhat, String userWhere, String userWhy, String userHow) {
        super(cause);
        this.userWhat = userWhat;
        this.userWhere = userWhere;
        this.userWhy = userWhy;
        this.userHow = userHow;
    }

    public String getUserWhat() {
        return userWhat;
    }

    public String getUserWhy() {
        return userWhy;
    }

    public String getUserHow() {
        return userHow;
    }

    @Override
    public String toString() {
        try {
            return "An error occurred @ " + userWhere + "\n" +
                    "What:\t" + userWhy + "\n" +
                    "Why:\t" + userWhat + "\n" +
                    "How to solve:\t" + userHow + "\n\n\n" +
                    getCause() != null ? super.toString() + "\n\n" + ExceptionUtils.getStackTrace(getCause()) : super.toString();
        }
        catch (Throwable e) {
            e.printStackTrace();
            return "An error occurred @ " + userWhere + "\n" +
                    "What:\t" + userWhy + "\n" +
                    "Why:\t" + userWhat + "\n" +
                    "How to solve:\t" + userHow + "\n\n\n" +
                    super.toString();
        }
    }

    public String getUserWhere() {
        return userWhere;
    }
}
