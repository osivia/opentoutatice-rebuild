/**
 * 
 */
package fr.toutatice.ecm.es.customizer.listeners.denormalization;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import fr.toutatice.ecm.es.customizer.listeners.api.ICustomESListener;
import fr.toutatice.ecm.es.customizer.nx.listener.ESInlineListenerCustomizer;


/**
 * @author david
 *
 */
public abstract class AbstractDenormalizationESListener implements ICustomESListener {
    
    private ESInlineListenerCustomizer esInlineListener;
    
    /**
     * Default constructor.
     */
    public AbstractDenormalizationESListener(){};


    /**
     * @return the esListener
     */
    public ESInlineListenerCustomizer getEsInlineListener() {
        return esInlineListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setESInlineListener(ESInlineListenerCustomizer esListener) {
        this.esInlineListener = esListener;
    }
    
    /**
     * @param sourceDocument
     * @return true if sourceDocument is linked with a document to re-index.
     */
    protected abstract boolean needToReIndex(DocumentModel sourceDocument, String eventId);

    protected boolean needToReIndex(DocumentEventContext context, String eventId) {
    	return needToReIndex(context.getSourceDocument(), eventId);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void customStackCommands(DocumentEventContext docCtx, String eventId) {

        if (needToReIndex(docCtx, eventId)) {
        	DocumentModel sourceDocument = docCtx.getSourceDocument();
            CoreSession session = sourceDocument.getCoreSession();
            stackCommands(session, sourceDocument, eventId);
        }
    }
    
    // FIXME: should be used in stackCommands.
//    /**
//     * @param linkedDocument
//     * @return true if re-indexation of document linked to linkedDocument
//     *         must be synchronous.
//     */
//    protected abstract boolean isSyncReIndexation(DocumentModel linkedDocument);
    
    /**
     * Stacks commands of reindexation of linked docs.
     * 
     * @param session
     * @param sourceDocument
     * @param eventId
     */
    protected abstract void stackCommands(CoreSession session, DocumentModel sourceDocument, String eventId);


}
