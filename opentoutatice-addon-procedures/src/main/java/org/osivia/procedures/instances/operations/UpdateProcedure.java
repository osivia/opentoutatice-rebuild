package org.osivia.procedures.instances.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.TaskService;
import org.osivia.procedures.instances.operations.runner.DocRefToDocModelUnrestrictedSessionRunner;

/**
 * Update procedure operation.
 *
 * @author Dorian Licois
 * @author Cédric Krommenhoek
 */
@Operation(id = UpdateProcedure.ID, category = Constants.CAT_SERVICES, label = "UpdateProcedure", description = "Updates a procedure.")
public class UpdateProcedure {

    /** Operation identifier. */
    public static final String ID = "Services.UpdateProcedure";


    /** Core session. */
    @Context
    private CoreSession session;

    /** Document routing service. */
    @Context
    private DocumentRoutingService documentRoutingService;

    /** Task service. */
    @Context
    private TaskService taskService;


    /** Task title parameter. */
    @Param(name = "taskTitle")
    private String taskTitle;

    /** Task properties parameter. */
    @Param(name = "properties", required = false)
    private Properties properties;

    /** Task actors parameter. */
    @Param(name = "actors", required = false)
    private StringList actors;

    /** Task additional authorizations parameter. */
    @Param(name = "additionalAuthorizations", required = false)
    private StringList additionalAuthorizations;


    /**
     * Constructor.
     */
    public UpdateProcedure() {
        super();
    }


    /**
     * Run operation.
     *
     * @param procedureInstance procedure instance
     * @return updated procedure instance
     * @throws Exception
     */
    @OperationMethod
    public DocumentModel run(DocumentModel procedureInstance) throws Exception {
        UpdateProcedureUnrestrictedSessionRunner unrestrictedSessionRunner = new UpdateProcedureUnrestrictedSessionRunner(session, procedureInstance,
                taskTitle, properties, actors, additionalAuthorizations);
        unrestrictedSessionRunner.runUnrestricted();
        return unrestrictedSessionRunner.getProcedureInstance();
    }

    @OperationMethod
    public DocumentModel run(DocumentRef procedureInstanceRef) throws Exception {
        DocRefToDocModelUnrestrictedSessionRunner docRefToDocModelUnrestrictedSessionRunner = new DocRefToDocModelUnrestrictedSessionRunner(session,
                procedureInstanceRef);

        docRefToDocModelUnrestrictedSessionRunner.runUnrestricted();

        return run(docRefToDocModelUnrestrictedSessionRunner.getDocument());
    }

}
