/**
 * 
 */
package fr.toutatice.ecm.es.customizer.writers.api;

import java.io.IOException;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.fasterxml.jackson.core.JsonGenerator;

import fr.toutatice.ecm.es.customizer.nx.writer.JsonESDocumentWriterCustomizer;


/**
 * @author david
 *
 */
public abstract class AbstractCustomJsonESWriter implements ICustomJsonESWriter {
    
    /**
     * Native Nx JsonWriter.
     */
    protected JsonESDocumentWriterCustomizer jsonESWriter;
    
    /** Session. */
    protected CoreSession session;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setJsonESWriter(JsonESDocumentWriterCustomizer jsonESWriter) {
        this.jsonESWriter = jsonESWriter;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentSession(DocumentModel doc){
            this.session = doc.getCoreSession();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void writeData(JsonGenerator jg, DocumentModel doc, String[] schemas, Map<String, String> contextParameters) throws IOException;
}
