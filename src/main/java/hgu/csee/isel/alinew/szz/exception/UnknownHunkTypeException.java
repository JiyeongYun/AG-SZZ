package hgu.csee.isel.alinew.szz.exception;

public class UnknownHunkTypeException extends Exception {

	public UnknownHunkTypeException() {
		this("ERROR - Unknown Hunk Type");
	}

	public UnknownHunkTypeException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public UnknownHunkTypeException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public UnknownHunkTypeException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public UnknownHunkTypeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
