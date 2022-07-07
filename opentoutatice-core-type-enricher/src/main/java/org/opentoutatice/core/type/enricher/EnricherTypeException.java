/**
 * 
 */
package org.opentoutatice.core.type.enricher;

import org.nuxeo.ecm.core.api.NuxeoException;


/**
 * @author david
 */
public class EnricherTypeException extends NuxeoException {

    private static final long serialVersionUID = 2991203684872782820L;

    public EnricherTypeException(String msg, Throwable e) {
        super(msg, e);
    }

}
