package irysc.gachesefid.Exception;

public class InvalidFieldsException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String message;

    public InvalidFieldsException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
