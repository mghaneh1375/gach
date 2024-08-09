package irysc.gachesefid.Exception;

public class InvalidFieldsException extends RuntimeException {

    private static final long serialVersionUID = -552136747506374233L;

    private final String message;

    public InvalidFieldsException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
