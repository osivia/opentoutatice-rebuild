/**
 * 
 */
package fr.toutatice.ecm.es.customizer.listeners.api;

import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import fr.toutatice.ecm.es.customizer.nx.listener.ESInlineListenerCustomizer;


/**
 * @author david
 *
 */
public interface ICustomESListener {
    
    /**
     * Set the root listener.
     * 
     * @param listener
     */
    void setESInlineListener(ESInlineListenerCustomizer esListener);
    
    /**
     * Custom indexing method.
     * 
     * @param docCtx
     * @param eventId
     */
    void customStackCommands(DocumentEventContext docCtx, String eventId);

}
