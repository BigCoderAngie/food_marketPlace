package na.eat.marketplace.service;

/** A rule was broken; the message is safe to show to the user. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
