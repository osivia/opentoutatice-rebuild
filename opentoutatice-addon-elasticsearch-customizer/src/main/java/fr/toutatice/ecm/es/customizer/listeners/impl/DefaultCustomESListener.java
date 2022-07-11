/**
 * 
 */
package fr.toutatice.ecm.es.customizer.listeners.impl;

import org.apache.commons.lang.ArrayUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.elasticsearch.commands.IndexingCommands;

import fr.toutatice.ecm.es.customizer.listeners.denormalization.AbstractDenormalizationESListener;


/**
 * Default Opentoutatice listener enabling listening of desired events
 * (for indexation).
 * 
 * @author david
 *
 */
public class DefaultCustomESListener extends AbstractDenormalizationESListener {
    
    /** Events inducing denormalized indexation. */
    private static String[] DENORMALIZING_EVENTS = {DocumentEventTypes.DOCUMENT_LOCKED, DocumentEventTypes.DOCUMENT_UNLOCKED};

    @Override
    protected boolean needToReIndex(DocumentModel sourceDocument, String eventId) {
        return ArrayUtils.contains(DENORMALIZING_EVENTS, eventId);
    }

    @Override
    protected void stackCommands(CoreSession session, DocumentModel sourceDocument, String eventId) {
        IndexingCommands cmds = this.getEsInlineListener().getOrCreateCommands(sourceDocument);
        cmds.add(Type.UPDATE, true, false);
    }



}
