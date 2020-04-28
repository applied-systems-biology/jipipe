package org.hkijena.acaq5.api.exceptions;

/**
 * A {@link RuntimeException} that contains more detailed information for users.
 * It answers in understandable terms: (1) What happened (2) Why it happened, and (3) What to do to resolve the issue
 */
public class UserFriendlyRuntimeException extends RuntimeException {
    private final String userWhat;
    private final String userWhy;
    private final String userHow;


    /**
     * @param cause    the underlying exception
     * @param message  the developer message
     * @param userWhat the message for the user
     * @param userWhy  explanation why it happened
     * @param userHow  explanation how to resolve the issue
     */
    public UserFriendlyRuntimeException(String message, Throwable cause, String userWhat, String userWhy, String userHow) {
        super(message, cause);
        this.userWhat = userWhat;
        this.userWhy = userWhy;
        this.userHow = userHow;
    }

    /**
     * @param message  the developer message
     * @param userWhat the message for the user
     * @param userWhy  explanation why it happened
     * @param userHow  explanation how to resolve the issue
     */
    public UserFriendlyRuntimeException(String message, String userWhat, String userWhy, String userHow) {
        super(message);
        this.userWhat = userWhat;
        this.userWhy = userWhy;
        this.userHow = userHow;
    }

    /**
     * @param cause    the underlying exception
     * @param userWhat the message for the user
     * @param userWhy  explanation why it happened
     * @param userHow  explanation how to resolve the issue
     */
    public UserFriendlyRuntimeException(Throwable cause, String userWhat, String userWhy, String userHow) {
        super(cause);
        this.userWhat = userWhat;
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
        return String.format("An error occurred!\n" +
                "What:\t%s\n" +
                "Why:\t%s\n" +
                "How to solve:\t%s\n\n%s", userWhat, userWhy, userHow, super.toString());
    }
}
