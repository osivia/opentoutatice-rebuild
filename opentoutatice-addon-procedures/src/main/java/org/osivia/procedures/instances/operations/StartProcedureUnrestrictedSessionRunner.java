package org.osivia.procedures.instances.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.commons.collections.CollectionUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Start procedure unrestricted session runner.
 *
 * @author Cédric Krommenhoek
 * @see AbstractProcedureUnrestrictedSessionRunner
 */
public class StartProcedureUnrestrictedSessionRunner extends AbstractProcedureUnrestrictedSessionRunner {

    /** Procedure instance. */
    private DocumentModel procedureInstance;


    /** Procedure initiator. */
    private final String procedureInitiator;

    /** Task title. */
    private final String title;
    /** Task properties. */
    private final Properties properties;
    /** Task actors: users and groups. */
    private final StringList actors;
    /** Task additional authorizations. */
    private final StringList additionalAuthorizations;

    /** Document routing service. */
    private final DocumentRoutingService documentRoutingService;


    /**
     * Constructor.
     *
     * @param session core session
     * @param procedureInitiator procedure initiator
     * @param title task title
     * @param properties task properties
     * @param actors task users and groups
     * @param additionalAuthorizations task additional authorizations
     */
    public StartProcedureUnrestrictedSessionRunner(CoreSession session, String procedureInitiator, String title, Properties properties, StringList actors,
            StringList additionalAuthorizations) {
        super(session, properties);
        this.procedureInitiator = procedureInitiator;
        this.title = title;
        this.properties = properties;
        this.actors = actors;
        this.additionalAuthorizations = additionalAuthorizations;

        documentRoutingService = Framework.getService(DocumentRoutingService.class);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        // Generic model
        DocumentModel genericModel = getGenericModel();

        // Procedure model
        DocumentModel model = getModel();

        // Procedure instance creation
        procedureInstance = createProcedureInstance(model);

        // Document identifiers
        List<String> identifiers = new ArrayList<>();
        identifiers.add(procedureInstance.getId());

        // Associate objects to workflow
        associateObject(procedureInstance, identifiers);

        // Create workflow
        String processId = documentRoutingService.createNewInstance(genericModel.getName(), identifiers, session, true);

        // Create task
        try {
			createTask(model, procedureInstance, processId, title, actors, additionalAuthorizations);
		} catch (LoginException e) {
			// TODO gestion erreur
		}
        
        
        
    }


    /**
     * Get generic model.
     *
     * @return generic model
     */
    private DocumentModel getGenericModel() {
        String id = documentRoutingService.getRouteModelDocIdWithId(session, "generic-model");
        DocumentRef ref = new IdRef(id);
        return session.getDocument(ref);
    }


    /**
     * Create procedure instance.
     *
     * @param model model
     * @return created procedure instance
     */
    private DocumentModel createProcedureInstance(DocumentModel model) {
        // Parent path
        DocumentModel procedureInstanceContainer = getProcedureInstanceContainer(model);
        String parentPath = procedureInstanceContainer.getPathAsString();

        // Create procedure instance model
        DocumentModel procedureInstanceModel = session.createDocumentModel(parentPath, model.getName(), "ProcedureInstance");

        // Create procedure instance based on model
        DocumentModel procedureInstance = session.createDocument(procedureInstanceModel);

        // Procedure initiator
        properties.put("pi:procedureInitiator", procedureInitiator);

        // Update procedure instance properties
        try {
            DocumentHelper.setProperties(session, procedureInstance, properties);
        } catch (IOException e) {
			// TODO gestion erreur
        }

        // Save document
        DocumentModel saveDocument = session.saveDocument(procedureInstance);
        
        // Force refresh because the datas can be accessed later without enough rights (JSonWriter)
        // and detach mode breaks transaction

        saveDocument.refresh(DocumentModel.REFRESH_ALL, saveDocument.getSchemas());
        saveDocument.getLockInfo();
        
        
         
        return saveDocument;
    }


    /**
     * Get procedure instance container.
     *
     * @param model model
     * @return procedure instance container
     */
    private DocumentModel getProcedureInstanceContainer(DocumentModel model) {
        // Model path
        Path modelPath = model.getPath();
        // Container path
        Path containerPath = modelPath.uptoSegment(modelPath.segmentCount() - 2);

        // Query
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM Document ");
        query.append("WHERE ecm:primaryType = 'ProceduresInstancesContainer' ");
        query.append("AND ecm:path STARTSWITH '").append(containerPath.toString()).append("' ");

        // Query execution
        DocumentModelList result = session.query(query.toString());

        // Procedure instance container
        DocumentModel procedureInstanceContainer = null;
        if (result.size() == 1) {
            procedureInstanceContainer = result.get(0);
        } else {
			// TODO gestion erreur
        }
        return procedureInstanceContainer;
    }


    /**
     * Associate objects to workflow.
     *
     * @param procedureInstance procedure instance
     * @param identifiers current document identifiers
     */
    private void associateObject(DocumentModel procedureInstance, List<String> identifiers) {
        List<?> procedureObjectInstancesList = procedureInstance.getProperty("pi:procedureObjectInstances").getValue(List.class);
        if (CollectionUtils.isNotEmpty(procedureObjectInstancesList)) {
            for (Object procedureObjectInstances : procedureObjectInstancesList) {
                Map<?, ?> procedureObjectInstancesMap = (Map<?, ?>) procedureObjectInstances;
                String procedureObjectId = (String) procedureObjectInstancesMap.get("procedureObjectId");
                if (procedureObjectId != null) {
                    identifiers.add(procedureObjectId);
                }
            }
        }
    }


    /**
     * Getter for procedureInstance.
     *
     * @return the procedureInstance
     */
    public DocumentModel getProcedureInstance() {
        return procedureInstance;
    }

}
