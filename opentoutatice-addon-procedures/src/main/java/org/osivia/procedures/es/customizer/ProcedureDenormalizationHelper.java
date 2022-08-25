/**
 *
 */
package org.osivia.procedures.es.customizer;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;
import org.osivia.procedures.constants.ProceduresConstants;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.core.query.helper.ToutaticeQueryHelper;


/**
 * @author david
 *
 */
public final class ProcedureDenormalizationHelper {

    /** Task query (as DocumentModel). */
    private static final String TASK_DOC_QUERY = "select * from TaskDoc where ecm:currentLifeCycleState not in ('ended', 'cancelled') "
            + "and ecm:isProxy = 0 and nt:targetDocumentId = '%s'";

    /** Singleton instance. */
    private static ProcedureDenormalizationHelper instance;

    /** Task Service. */
    protected static TaskService taskService;

    /**
     * Singleton class.
     */
    private ProcedureDenormalizationHelper() {
        super();
    }

    /**
     * Getter for ProcedureDenormalizationHelper instance.
     *
     * @return instance of ProcedureDenormalizationHelper.
     */
    public static synchronized ProcedureDenormalizationHelper getInstance() {
        if (instance == null) {
            instance = new ProcedureDenormalizationHelper();
        }
        return instance;
    }

    /** Getter for Task Service. */
    public static TaskService getTaskService() {
        if (taskService == null) {
            taskService = Framework.getService(TaskService.class);
        }
        return taskService;
    }

    /**
     * Get Task document model associated with given procedureInstance.
     *
     * @return Task document model associated with given procedureInstance.
     */
    public DocumentModel getTaskOfProcedureInstance(CoreSession session, DocumentModel pi) {
        String query = String.format(TASK_DOC_QUERY, pi.getId());
        DocumentModelList tasks = null;

        if ((session.getPrincipal() != null) && !((NuxeoPrincipal) session.getPrincipal()).isAdministrator()) {
            // Listener case
            tasks = ToutaticeQueryHelper.queryUnrestricted(session, query);
        } else {
            // Writer case
            tasks = session.query(query);
        }

        if ((tasks != null) && (tasks.size() == 1)) {
            return tasks.get(0);
        }
        return null;
    }

    /**
     * Get ProcedureInstance linked to given TaskDoc.
     *
     * @param doc
     * @return ProcedureInstance.
     */
    public DocumentModel getProcedureInstanceOfTask(CoreSession session, DocumentModel taskDoc) {
        Task task = taskDoc.getAdapter(Task.class);
        DocumentModel targetDocumentModel = null;

        if ((session.getPrincipal() != null) && !((NuxeoPrincipal) session.getPrincipal()).isAdministrator()) {
            // Listener case
        	List<String> targetDocumentsIds = task.getTargetDocumentsIds();

        	// TODO use first item
            targetDocumentModel = ToutaticeDocumentHelper.getUnrestrictedDocument(session, targetDocumentsIds.get(0));
        } else {
            // Writer case
            targetDocumentModel = getTaskService().getTargetDocumentModel(task, session);
        }

        return (targetDocumentModel != null) && StringUtils.equals(ProceduresConstants.PI_TYPE, targetDocumentModel.getType()) ? targetDocumentModel : null;
    }

}
