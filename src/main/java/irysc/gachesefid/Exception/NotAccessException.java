package irysc.gachesefid.Exception;

public class NotAccessException extends Exception {

    private static final long serialVersionUID = 58L;

    private final String message;

    public NotAccessException(String message) {
        this.message = message;
    }

    public NotAccessException() {
        this.message = "not access";
    }

    @Override
    public String getMessage() {
        return message;
    }

}
