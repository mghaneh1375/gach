package irysc.gachesefid.Exception;

public class ExcessMaxException extends Exception {

    private static final long serialVersionUID = 2L;

    private final String message;

    public ExcessMaxException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
