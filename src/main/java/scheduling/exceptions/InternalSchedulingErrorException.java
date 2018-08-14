package scheduling.exceptions;


public class InternalSchedulingErrorException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InternalSchedulingErrorException() {
        super();

    }

    public InternalSchedulingErrorException(String message) {
        super(message);
    }

}
