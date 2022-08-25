/**
 * 
 */
package org.osivia.procedures.instances.operations.runner;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.osivia.procedures.exception.ProcedureException;
import org.osivia.procedures.utils.ProcedureHelper;


/**
 * @author david
 *
 */
public class UnrestrictedCancelProcedure extends UnrestrictedSessionRunner {

    /** Procedure instance. */
    private DocumentModel procedureInstance;
    /** Delete Procedure instance indicator. */
    private boolean deletePi;
    /** Engine service. */
    private DocumentRoutingEngineService engineService;
    
    /**
     * Constructor.
     * 
     * @param routingService
     * @param engineService
     * @param session
     * @param procedureInstance
     * @param deletePi
     */
    public UnrestrictedCancelProcedure(CoreSession session, DocumentRoutingService routingService, DocumentRoutingEngineService engineService, 
            DocumentModel procedureInstance, boolean deletePi) {
        super(session);
        this.procedureInstance = procedureInstance;
        this.deletePi = deletePi;
        this.engineService = engineService;
    }
    
    /**
     * Cancels procedure and delete ProcedureInstance if indicated.
     */
    @Override
    public void run() {
        try {
			cancelProcedure(this.session, this.engineService, this.procedureInstance);
		} catch (ProcedureException e) {
			// TODO Auto-generated catch block
			
		}
        if(this.deletePi){
            removeProcedureInstance(this.session, this.procedureInstance);
        }
    }
    
    /**
     * Cancels a procedure (ready or running) associated 
     * with given procedure instance.
     * 
     * @param session
     * @param routingService
     * @param engineService
     * @param procedureInstance
     * @throws ProcedureException
     */
    public void cancelProcedure(CoreSession session, DocumentRoutingEngineService engineService, DocumentModel procedureInstance) throws ProcedureException {
        DocumentRoute routeInstance = ProcedureHelper.getInstance().getProcedureAsRoute(session, procedureInstance);
        if(routeInstance.getDocument() != null){
            engineService.cancel(routeInstance, session);
        } else {
            throw new ProcedureException("No workflow process associated with ProcedureInstance: ".concat(procedureInstance.getId()));
        }
    }
    
    /**
     * Removes given procedure instance.
     * 
     * @param session
     * @param procedureInstance
     */
    public void removeProcedureInstance(CoreSession session, DocumentModel procedureInstance) {
       session.removeDocument(procedureInstance.getRef());
    }
    
}
