package scheduling.exceptions;

public class RoutingNotFoundException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RoutingNotFoundException() {
        super();

    }

    public RoutingNotFoundException(String message, Throwable cause) {
        super(message, cause);

    }

    public RoutingNotFoundException(String message) {
        super(message);
    }

    public RoutingNotFoundException(Throwable cause) {
        super(cause);
    }
}
