package irysc.gachesefid.Exception;

public class NotActivateAccountException extends Exception {

    private static final long serialVersionUID = 51L;

    private final String message;

    public NotActivateAccountException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
