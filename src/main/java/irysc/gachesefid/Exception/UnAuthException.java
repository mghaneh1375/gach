package irysc.gachesefid.Exception;

public class UnAuthException extends Exception {

    private static final long serialVersionUID = 54L;

    private final String message;

    public UnAuthException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
