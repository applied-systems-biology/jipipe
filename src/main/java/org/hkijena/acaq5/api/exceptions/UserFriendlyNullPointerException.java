package org.hkijena.acaq5.api.exceptions;

/**
 * A {@link RuntimeException} that contains more detailed information for users.
 * It answers in understandable terms: (1) What happened (2) Where it happened (3) Why it happened, and (4) What to do to resolve the issue
 */
public class UserFriendlyNullPointerException extends NullPointerException implements UserFriendlyException {
    private final String userWhat;
    private final String userWhere;
    private final String userWhy;
    private final String userHow;


    /**
     * @param message   the developer message
     * @param userWhat  the message for the user
     * @param userWhere location of the exception
     * @param userWhy   explanation why it happened
     * @param userHow   explanation how to resolve the issue
     */
    public UserFriendlyNullPointerException(String message, String userWhat, String userWhere, String userWhy, String userHow) {
        super(message);
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
        return String.format("An error occurred @ " + userWhere + "\n" +
                "What:\t%s\n" +
                "Why:\t%s\n" +
                "How to solve:\t%s\n\n%s", userWhat, userWhy, userHow, super.toString());
    }

    public String getUserWhere() {
        return userWhere;
    }
}
