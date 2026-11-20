package ats.domain;

/** Thrown when a system operation cannot be carried out; the message is shown to the user. */
public class BookingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BookingException(String message) {
        super(message);
    }
}
