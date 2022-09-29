/**
 *
 */
package fr.toutatice.ecm.platform.automation.trash;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;


/**
 * Restore documents operation.
 *
 * @see AbstractTrashOperation
 * @author David Chevrier
 * @author Cédric Krommenhoek
 */
@Operation(id = RestoreDocuments.ID, category = Constants.CAT_SERVICES, label = "RestoreDocuments", description = "Restore (selected) documents in trash.")
public class RestoreDocuments extends AbstractTrashOperation {

    /** Operation identifier. */
    public static final String ID = "Services.RestoreDocuments";

    /** Core session. */
    @Context
    protected CoreSession session;

    /** Parent parameter. */
    @Param(name = "parent", required = false)
    protected DocumentModel parent;


    /**
     * Constructor.
     */
    public RestoreDocuments() {
        super();
    }


    /**
     * Run operation.
     *
     * @return rejected documents
     * @throws Exception
     */
    @OperationMethod
    public DocumentModelList run() throws Exception {
        return this.execute(this.session, this.parent);
    }


    /**
     * Run operation.
     *
     * @param documents documents
     * @return rejected documents
     * @throws Exception
     */
    @OperationMethod
    public DocumentModelList run(DocumentModelList documents) throws Exception {
        return this.execute(this.session, documents);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(TrashService service, TrashInfo info) throws Exception {
        service.undeleteDocuments(info.docs);
    }

}
