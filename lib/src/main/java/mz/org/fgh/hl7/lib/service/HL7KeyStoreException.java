package mz.org.fgh.hl7.lib.service;

public class HL7KeyStoreException extends RuntimeException {
    public HL7KeyStoreException() {
        super();
    }

    public HL7KeyStoreException(String message) {
        super(message);
    }

    public HL7KeyStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public HL7KeyStoreException(Throwable cause) {
        super(cause);
    }
}
