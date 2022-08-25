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
public class ProcedureTaskDenormalizationESListener extends AbstractDenormalizationESListener {
    
    /** Is document a TaskDoc. */
    private boolean isTaskDoc = false;
    /** Is document a Procedureinstance. */
    private boolean isPI = false;
    
    /**
     * Default constructor.
     */
    public ProcedureTaskDenormalizationESListener() {
        super();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected boolean needToReIndex(DocumentModel sourceDocument, String eventId) {
        // Base events: FIXME: set them by default
        isTaskDoc = TaskConstants.TASK_TYPE_NAME.equals(sourceDocument.getType());
        
        // Denormalization events
        isPI = ProceduresConstants.PI_TYPE.equals(sourceDocument.getType());
        
        return isTaskDoc || isPI;
    }


    @Override
    protected void stackCommands(CoreSession session, DocumentModel sourceDocument, String eventId) {
        if(isPI){
            DocumentModel task = ProcedureDenormalizationHelper.getInstance().getTaskOfProcedureInstance(session, sourceDocument);
            if(task != null){
                super.getEsInlineListener().stackCommand(task, eventId, true);
            }
        } else if(sourceDocument != null && isTaskDoc){
            super.getEsInlineListener().stackCommand(sourceDocument, eventId, true);
        }
    }

}
