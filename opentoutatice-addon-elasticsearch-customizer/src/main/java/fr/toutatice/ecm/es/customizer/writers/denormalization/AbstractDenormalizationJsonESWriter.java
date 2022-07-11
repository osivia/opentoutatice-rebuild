/**
 * 
 */
package fr.toutatice.ecm.es.customizer.writers.denormalization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;

import com.fasterxml.jackson.core.JsonGenerator;

import fr.toutatice.ecm.es.customizer.writers.api.AbstractCustomJsonESWriter;

/**
 * @author david
 *
 */
public abstract class AbstractDenormalizationJsonESWriter extends AbstractCustomJsonESWriter {
    
    /** First denormalization key for contextParameters map. */
    private static final String FIRST_CALL_KEY = "d_first_call";
    /** First call contextParameters map. */
    private static final String FIRST_CALL_TRUE = "dfc_TRUE";
    /** First call contextParameters map. */
    private static final String FIRST_CALL_FALSE = "dfc_FALSE";
    
    /** Caller key for contextParameters map. */
    private static final String INITIAL_CALLER_KEY = "d_initial_caller";
	
	/**
	 * Default constructor.
	 */
	public AbstractDenormalizationJsonESWriter(){
	    super();
	};
	
	/**
	 * @param doc current doc in indexing process.
	 * @return true if doc must be denormalize.
	 */
	// FIXME: set isNotDeleted and isNotVersion as params (nuxeo.conf?)
	protected boolean hasToDenormalize(DocumentModel doc, Map<String, String> contextParameters){
		if(doc == null){
			return false;
		}
		
		// Case of mutual denormalization and denormalizeDoc call json.wrtieESDoc
		boolean callItSelf = false;
		if(contextParameters != null){
		    callItSelf = StringUtils.equals(contextParameters.get(INITIAL_CALLER_KEY), doc.getType());
		}
		
		// Default rules
		boolean isNotDeleted = !doc.getLifeCyclePolicy().equalsIgnoreCase(LifeCycleConstants.DELETED_STATE);
		boolean isNotVersion = !doc.isVersion();
		boolean hasToDenormalize = isNotDeleted && isNotVersion && accept(doc);
		
		return hasToDenormalize && !callItSelf;
	}
	
	@Override
	public void writeData(JsonGenerator jg, DocumentModel doc, String[] schemas,
            Map<String, String> contextParameters) throws IOException {
		if(hasToDenormalize(doc, contextParameters)){
		    contextParameters = setCaller(doc, contextParameters);
			denormalizeDoc(jg, doc, schemas, contextParameters);
		}
		clearCaller(contextParameters);
	}
	
	
	
    /**
	 * Set doc as caller of denormalization.
	 * 
	 * @param doc
	 * @param contextParameters
	 */
	// for case of mutual denormalization and denormalizeDoc call json.wrtieESDoc 
	// (which call custom write method).
	private Map<String, String> setCaller(DocumentModel doc, Map<String, String> contextParameters) {
	    
	    if(contextParameters == null){
	        contextParameters = new HashMap<String, String>(2);
	        contextParameters.put(FIRST_CALL_KEY, FIRST_CALL_TRUE);
    	} 
	    
	    if(StringUtils.equals(FIRST_CALL_TRUE, contextParameters.get(FIRST_CALL_KEY))){
	        contextParameters.put(INITIAL_CALLER_KEY, doc.getType());
	    }
	    
        contextParameters.put(FIRST_CALL_KEY, FIRST_CALL_FALSE);
        
        return contextParameters;
    }
	
	/**
	 * Clear caller of denormalization.
	 * 
	 * @param contextParameters
	 */
	private void clearCaller(Map<String, String> contextParameters) {
        if (contextParameters != null) {
            contextParameters.remove(INITIAL_CALLER_KEY);
            contextParameters.put(FIRST_CALL_KEY, FIRST_CALL_TRUE);
        }
	}

    /**
	 * Denormalize given doc. 
	 * @param doc doc to denormalize.
	 */
	protected abstract void denormalizeDoc(JsonGenerator jg, DocumentModel doc, String[] schemas,
            Map<String, String> contextParameters) throws IOException;
	
}
