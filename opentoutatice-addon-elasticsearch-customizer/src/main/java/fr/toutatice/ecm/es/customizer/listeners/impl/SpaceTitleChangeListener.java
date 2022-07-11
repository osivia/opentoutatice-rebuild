/**
 * 
 */
package fr.toutatice.ecm.es.customizer.listeners.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.elasticsearch.commands.IndexingCommands;

import fr.toutatice.ecm.es.customizer.listeners.denormalization.AbstractDenormalizationESListener;
import fr.toutatice.ecm.es.customizer.nx.listener.TitleModificationListener;

/**
 * If TitleModificationListener has flagged the document context, this listerner stacks commands
 * after document modification to propagate the space title modification is all subdocument.
 * 
 * @author Loïc Billon
 *
 */
public class SpaceTitleChangeListener  extends AbstractDenormalizationESListener {

    private Log log = LogFactory.getLog(SpaceTitleChangeListener.class);

	
	/* (non-Javadoc)
	 * @see fr.toutatice.ecm.es.customizer.listeners.denormalization.AbstractDenormalizationESListener#needToReIndex(org.nuxeo.ecm.core.api.DocumentModel, java.lang.String)
	 */
	@Override
	protected boolean needToReIndex(DocumentModel sourceDocument, String eventId) {

		// Not used
		
		return false;
	}
	
	/* (non-Javadoc)	 * @see fr.toutatice.ecm.es.customizer.listeners.denormalization.AbstractDenormalizationESListener#needToReIndex(org.nuxeo.ecm.core.event.impl.DocumentEventContext, java.lang.String)
	 */
	@Override
	protected boolean needToReIndex(DocumentEventContext context, String eventId) {
		
		if(context.getProperties().containsKey(TitleModificationListener.DOC_CONTEXT_TITLE_MODIFICATION)) {
			log.debug("needToReIndex "+context.getSourceDocument().getTitle() +" on event "+eventId);
			
			return true;
		}
		
		else return false;
	}

	/* (non-Javadoc)
	 * @see fr.toutatice.ecm.es.customizer.listeners.denormalization.AbstractDenormalizationESListener#stackCommands(org.nuxeo.ecm.core.api.CoreSession, org.nuxeo.ecm.core.api.DocumentModel, java.lang.String)
	 */
	@Override
	protected void stackCommands(CoreSession session, DocumentModel sourceDocument, String eventId) {
		
        IndexingCommands cmds = this.getEsInlineListener().getOrCreateCommands(sourceDocument);
        cmds.add(Type.UPDATE, false, true);

		
	}

	
}
