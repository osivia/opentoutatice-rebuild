/**
 *
 */
package org.osivia.procedures.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.routing.core.impl.DocumentRouteImpl;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskImpl;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;


/**
 * @author david
 *
 */
public class ProcedureHelper {

    /** Routing service. */
    private DocumentRoutingService routingService;
    /** Routing engine service. */
    private DocumentRoutingEngineService engineService;
    /** Task service. */
    private TaskService taskService;

    /** ProcedureHelper instance. */
    private static ProcedureHelper instance;

    public static final String WEB_ID_QUERY = "SELECT * FROM Document WHERE ttc:webid = '";

    public static final String PROC_INSTANCE_CONTAINER_QUERY = "SELECT * FROM ProceduresInstancesContainer where ecm:path STARTSWITH '";

    /**
     * Singleton.
     */
    private ProcedureHelper() {
        super();
        routingService = Framework.getService(DocumentRoutingService.class);
        engineService = Framework.getService(DocumentRoutingEngineService.class);
        taskService = Framework.getService(TaskService.class);;
    }

    /**
     * Getter for ProcedureHelper instance.
     *
     * @return ProcedureHelper instance.
     */
    public synchronized static ProcedureHelper getInstance(){
        if(instance == null){
            instance = new ProcedureHelper();
        }
        return instance;
    }

    /**
     * Gets procedure (ready or running) as DocumentRoute.
     *
     * @return procedure as DocumentRoute.
     */
    public DocumentRoute getProcedureAsRoute(CoreSession session, DocumentModel procedureInstance) {
        List<DocumentRoute> documentRoutes = routingService.getDocumentRoutesForAttachedDocument(session, procedureInstance.getId());
        if(CollectionUtils.isNotEmpty(documentRoutes)){
            return documentRoutes.get(0);
        }
        return new DocumentRouteImpl(null,  null);
    }

    /**
     * Gets the current task for given procedure
     * and given actors.
     *
     * @param session
     * @param procedureInstance
     * @param actors
     * @return
     */
    public Task getCurrentTask(CoreSession session, DocumentModel procedureInstance, List<String> actors) {
        List<Task> taskInstances = taskService.getTaskInstances(procedureInstance, actors, session);
        if((taskInstances != null) && (taskInstances.size() > 1)){
            return taskInstances.get(0);
        }
        return new TaskImpl(null);
    }

    /**
     * @param procedureInstanceWebId
     * @param stepMap
     * @return
     */
    public static ArrayList<Map<String, Serializable>> buildTaskVariables(String procedureInstanceWebId, Map<String, Object> stepMap) {
        ArrayList<Map<String, Serializable>> stepTaskVariables;
        stepTaskVariables = new ArrayList<Map<String, Serializable>>(9);
        Map<String, Serializable> taskVariableNotifiable = new HashMap<String, Serializable>(2);
        taskVariableNotifiable.put("key", "notifiable");
        taskVariableNotifiable.put("value", BooleanUtils.toStringTrueFalse((Boolean) stepMap.get("notifiable")));
        stepTaskVariables.add(taskVariableNotifiable);

        Map<String, Serializable> taskVariableNotifEmail = new HashMap<String, Serializable>(2);
        taskVariableNotifEmail.put("key", "notifEmail");
        taskVariableNotifEmail.put("value", BooleanUtils.toStringTrueFalse((Boolean) stepMap.get("notifEmail")));
        stepTaskVariables.add(taskVariableNotifEmail);

        Map<String, Serializable> taskVariableAcquitable = new HashMap<String, Serializable>(2);
        taskVariableAcquitable.put("key", "acquitable");
        taskVariableAcquitable.put("value", BooleanUtils.toStringTrueFalse((Boolean) stepMap.get("acquitable")));
        stepTaskVariables.add(taskVariableAcquitable);

        Map<String, Serializable> taskVariableClosable = new HashMap<String, Serializable>(2);
        taskVariableClosable.put("key", "closable");
        taskVariableClosable.put("value", BooleanUtils.toStringTrueFalse((Boolean) stepMap.get("closable")));
        stepTaskVariables.add(taskVariableClosable);

        Map<String, Serializable> taskVariableactionIdClosable = new HashMap<String, Serializable>(2);
        taskVariableactionIdClosable.put("key", "actionIdClosable");
        taskVariableactionIdClosable.put("value", (String) stepMap.get("actionIdClosable"));
        stepTaskVariables.add(taskVariableactionIdClosable);

        Map<String, Serializable> taskVariableActionIdYes = new HashMap<String, Serializable>(2);
        taskVariableActionIdYes.put("key", "actionIdYes");
        taskVariableActionIdYes.put("value", (String) stepMap.get("actionIdYes"));
        stepTaskVariables.add(taskVariableActionIdYes);

        Map<String, Serializable> taskVariableActionIdNo = new HashMap<String, Serializable>(2);
        taskVariableActionIdNo.put("key", "actionIdNo");
        taskVariableActionIdNo.put("value", (String) stepMap.get("actionIdNo"));
        stepTaskVariables.add(taskVariableActionIdNo);

        Map<String, Serializable> taskVariableStringMsg = new HashMap<String, Serializable>(2);
        taskVariableStringMsg.put("key", "stringMsg");
        taskVariableStringMsg.put("value", (String) stepMap.get("stringMsg"));
        stepTaskVariables.add(taskVariableStringMsg);

        Map<String, Serializable> taskVariableProcedureInstanceWebId = new HashMap<String, Serializable>(2);
        taskVariableProcedureInstanceWebId.put("key", "documentWebId");
        taskVariableProcedureInstanceWebId.put("value", procedureInstanceWebId);
        stepTaskVariables.add(taskVariableProcedureInstanceWebId);
        return stepTaskVariables;
    }

}
