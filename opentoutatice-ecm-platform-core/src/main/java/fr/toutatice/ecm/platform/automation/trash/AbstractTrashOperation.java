package fr.toutatice.ecm.platform.automation.trash;

import java.security.Principal;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

/**
 * Trash operation abstract super-class.
 *
 * @author Cédric Krommenhoek
 */
public abstract class AbstractTrashOperation {

    /**
     * Constructor.
     */
    public AbstractTrashOperation() {
        super();
    }

    /**
     * Gets recursive trash info.
     * <b>FOR TEST: DO NOT USE!</b>
     * 
     * @return trash info
     */
    protected TrashInfo getRecursiveTrashInfo(TrashService trashService, CoreSession session, List<DocumentModel> docs) {
        // Result
        TrashInfo trashInfo = null;

        if (docs != null) {
            // Info of roots
            trashInfo = trashService.getTrashInfo(docs, session.getPrincipal(), false, false);
            // Recursive treatment
            if (trashInfo.forbidden > 0) {
                return trashInfo;
            } else {
                for (DocumentModel doc : docs) {
                    DocumentModelList children = session.getChildren(doc.getRef(), null, SecurityConstants.REMOVE);
                    trashInfo = getRecursiveTrashInfo(trashService, session, children);
                }
            }


        }

        return trashInfo;
    }


    /**
     * Execute operation.
     *
     * @param session core session
     * @param parent parent document
     * @throws Exception
     */
    public DocumentModelList execute(CoreSession session, DocumentModel parent) throws Exception {
        if (parent == null) {
            throw new Exception("\"parent\" parameter is undefined.");
        } else {
            // Parent identifier
            String parentId = parent.getId();

            // Get documents
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM Document ");
            query.append("WHERE ecm:ancestorId = '").append(parentId).append("' ");
            query.append("AND ecm:currentLifeCycleState = 'deleted' ");
            query.append("AND ecm:isProxy = 0 ");
            query.append("AND ecm:isVersion = 0 ");
            DocumentModelList documents = session.query(query.toString());

            return this.execute(session, documents);
        }
    }


    /**
     * Execute operation.
     *
     * @param session core session
     * @param documents documents
     * @throws Exception
     */
    public DocumentModelList execute(CoreSession session, DocumentModelList documents) throws Exception {
        // Trash service
        TrashService trashService = Framework.getService(TrashService.class);
        // Principal
        NuxeoPrincipal principal = session.getPrincipal();

        // Trash info
        TrashInfo info = trashService.getTrashInfo(documents, principal, false, false);

        // Rejected documents
        DocumentModelList rejected = new DocumentModelListImpl(info.forbidden);
        if (info.forbidden > 0) {
            for (DocumentModel document : documents) {
                if (!info.docs.contains(document)) {
                    rejected.add(document);
                }
            }
        }

        // Service invocation
        this.invoke(trashService, info);

        return rejected;
    }


    /**
     * Invoke trash service.
     *
     * @param service trash service
     * @param info trash info
     * @throws Exception
     */
    public abstract void invoke(TrashService service, TrashInfo info) throws Exception;

}
