package irysc.gachesefid.Exception;

public class NotCompleteAccountException extends Exception {

    private static final long serialVersionUID = 52L;

    private final String message;

    public NotCompleteAccountException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
