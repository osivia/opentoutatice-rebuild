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
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.TaskService;


/**
 * Start procedure operation.
 *
 * @author Dorian Licois
 * @author Cédric Krommenhoek
 */
@Operation(id = StartProcedure.ID, category = Constants.CAT_SERVICES, label = "StartProcedure", description = "Starts a procedure.")
public class StartProcedure {

    /** Operation identifier. */
    public static final String ID = "Services.StartProcedure";


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
    public StartProcedure() {
        super();
    }


    /**
     * Run operation.
     *
     * @return procedure instance
     * @throws Exception
     */
    @OperationMethod
    public DocumentModel run() throws Exception {
        return execute();
    }


    /**
     * Execute operation.
     *
     * @param blobList BLOB list
     * @return procedure instance
     * @throws Exception
     */
    private DocumentModel execute() throws Exception {
        // Procedure initiator
        String procedureInitiator = session.getPrincipal().getName();

        // Unrestricted session runner
        StartProcedureUnrestrictedSessionRunner unrestrictedSessionRunner = new StartProcedureUnrestrictedSessionRunner(session, procedureInitiator, taskTitle,
                properties, actors, additionalAuthorizations);
        unrestrictedSessionRunner.runUnrestricted();

        return unrestrictedSessionRunner.getProcedureInstance();
    }

}
