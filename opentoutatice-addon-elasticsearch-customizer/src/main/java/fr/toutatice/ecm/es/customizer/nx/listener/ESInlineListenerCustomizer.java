/**
 * 
 */
package fr.toutatice.ecm.es.customizer.nx.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.elasticsearch.commands.IndexingCommands;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.es.customizer.listeners.api.ICustomESListener;
import fr.toutatice.ecm.es.customizer.registry.ESCustomizersServiceRegistry;


/**
 * @author david
 *
 */
public class ESInlineListenerCustomizer extends ElasticSearchInlineListener {
    
    private Log log = LogFactory.getLog(ESInlineListenerCustomizer.class);
    
    /**
     * Registry of customizers.
     */
    private static ESCustomizersServiceRegistry registry;
    
    /**
     * @return registry of customizers.
     */
    public static ESCustomizersServiceRegistry getESCustomizersServiceRegistry() {
        if (registry == null) {
            registry = Framework.getService(ESCustomizersServiceRegistry.class);
        }
        return registry;
    }
    
    /**
     * Override method to add custom stack commands.
     */
    @Override
    public void handleEvent(Event event) {
        super.handleEvent(event);
        
        String eventId = event.getName();
        try{
            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
            for(ICustomESListener customListener : getESCustomizersServiceRegistry().getCustomESListeners()){
                customListener.setESInlineListener(this);
                customListener.customStackCommands(docCtx, eventId);
            }
        } catch (Exception e) {
            if (e instanceof ClassCastException){
                log.error("Event ".concat(eventId).concat(" is not in a DocumentEventContext."));
            }
        }
    }
    
    /*
     * To be visible in custom listeners.
     */
    public void stackCommand(DocumentModel doc, String eventId, boolean sync) {
        super.stackCommand(doc, eventId, sync);
    }
    
    /*
     * To be visible in custom listeners.
     */
    public IndexingCommands getOrCreateCommands(DocumentModel doc) {
        return super.getOrCreateCommands(doc);
    }

}
