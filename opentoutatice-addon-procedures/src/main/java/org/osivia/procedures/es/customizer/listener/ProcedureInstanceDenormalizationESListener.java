/**
 *
 */
package org.osivia.procedures.es.customizer.listener;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.osivia.procedures.constants.ProceduresConstants;
import org.osivia.procedures.es.customizer.ProcedureDenormalizationHelper;

import fr.toutatice.ecm.es.customizer.listeners.denormalization.AbstractDenormalizationESListener;


/**
 * @author david
 *
 */
public class ProcedureInstanceDenormalizationESListener extends AbstractDenormalizationESListener {
    
    /** Is document a TaskDoc. */
    private boolean isTaskDoc = false;
    /** Is document a Procedureinstance. */
    private boolean isPI = false;

    /**
     * Default constructor.
     */
    public ProcedureInstanceDenormalizationESListener() {
        super();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected boolean needToReIndex(DocumentModel sourceDocument, String eventId) {
        // Base events: FIXME: set them by default
        isPI = ProceduresConstants.PI_TYPE.equals(sourceDocument.getType());
        
        // Denormalization events
        isTaskDoc = TaskConstants.TASK_TYPE_NAME.equals(sourceDocument.getType());
        
        return isPI || isTaskDoc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void stackCommands(CoreSession session, DocumentModel doc, String eventId) {
        if(isTaskDoc){
            DocumentModel pi = ProcedureDenormalizationHelper.getInstance().getProcedureInstanceOfTask(session, doc);
            if(pi != null){
                super.getEsInlineListener().stackCommand(pi, eventId, true);
            }
        } else if(doc != null && isPI){
            super.getEsInlineListener().stackCommand(doc, eventId, true);
        }
    }

}
