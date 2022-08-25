/**
 * 
 */
package org.osivia.procedures.exception;

/**
 * Specialization of ClentException for Procedures.
 * 
 * @author david
 *
 */
public class ProcedureException extends Exception {

    /** Serial id. */
    private static final long serialVersionUID = 3955754966854474251L;

    /**
     * Default contructor.
     */
    public ProcedureException() {
        super();
    }

    /**
     * Constructor with message.
     * 
     * @param message
     */
    public ProcedureException(String message) {
        super(message);
    }

    /**
     * Constructor with cause as Throwable.
     * 
     * @param cause
     */
    public ProcedureException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with cause as ClientException.
     * 
     * @param cause
     */
    public ProcedureException(Exception cause) {
        super(cause);
    }

    /**
     * Constructor with message and ClientException cause.
     * 
     * @param message
     * @param cause
     */
    public ProcedureException(String message, Exception cause) {
        super(message, cause);
    }

    /**
     * Constructor with message and Throwable cause.
     * 
     * @param message
     * @param cause
     */
    public ProcedureException(String message, Throwable cause) {
        super(message, cause);
    }

}
