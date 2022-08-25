/**
 * 
 */
package org.osivia.procedures.instances.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.osivia.procedures.instances.operations.runner.UnrestrictedCancelProcedure;


/**
 * @author david
 *
 */
@Operation(id = CancelProcedure.ID, category = Constants.CAT_SERVICES, label = "CancelProcedure", description = "Cancels a procedure.")
public class CancelProcedure {
    
    /** Operation's id. */
    public static final String ID = "Services.CancelProcedure";
    
    /** Core Session. */
    @Context
    protected CoreSession session;
    
    /** Routing service. */
    @Context
    protected DocumentRoutingService routingService;
    
    /** Routing engine service. */
    @Context
    protected DocumentRoutingEngineService engineService;
    
    /** Indicates if we must delete the procedure instance
     *  when canceling procedure.
     *  Deleted by default.
     */
    @Param(name = "deletePi", required = false)
    private boolean deletePi = true;

    /**
     * Cancels a procedure.
     * 
     * @return
     * @throws Exception
     */
    @OperationMethod
    public void run(DocumentModel procedureInstance) throws Exception {
        UnrestrictedCancelProcedure unrestrictedStartProcedure = new UnrestrictedCancelProcedure(session, routingService, engineService,
                procedureInstance, deletePi);
        unrestrictedStartProcedure.runUnrestricted();
    }
    
}
