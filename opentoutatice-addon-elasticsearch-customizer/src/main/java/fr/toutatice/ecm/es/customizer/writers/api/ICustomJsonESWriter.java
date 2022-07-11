/**
 * 
 */
package fr.toutatice.ecm.es.customizer.writers.api;

import java.io.IOException;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.fasterxml.jackson.core.JsonGenerator;

import fr.toutatice.ecm.es.customizer.nx.writer.JsonESDocumentWriterCustomizer;

/**
 * @author david
 *
 */
public interface ICustomJsonESWriter {
	
	/**
	 * Setter of native Nx Json ES Writer.
	 * @param nxJsonESWriter
	 */
	void setJsonESWriter(JsonESDocumentWriterCustomizer jsonESWriter);
	
	 /**
     * Set current session (system if asynchronous session,
     * user session if synchronous session).
     * 
     * @param doc
     */
    void setCurrentSession(DocumentModel doc);
	
	/**
	 * Check if given doc must be cutomize in Json flux.
	 * 
	 * @param doc
	 * @return true if cutomize.
	 */
	boolean accept(DocumentModel doc);
	
	/**
	 * Method to add custom data in ES Json flux.
	 * 
	 * @param jg
	 * @param doc
	 * @param schemas
	 * @param contextParameters
	 */
	public void writeData(JsonGenerator jg, DocumentModel doc, String[] schemas,
            Map<String, String> contextParameters) throws IOException;

}
