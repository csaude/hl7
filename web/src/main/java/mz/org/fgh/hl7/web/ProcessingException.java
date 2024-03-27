package mz.org.fgh.hl7.web;

public class ProcessingException extends AppException {

    public ProcessingException(String message) {
        super(message);
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
