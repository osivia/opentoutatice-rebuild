/**
 *
 */
package org.osivia.procedures.instances.listener;

import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.osivia.procedures.constants.ProceduresConstants;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;


/**
 * @author david
 *
 */
public class ProcedureInstanceCreatedListener implements EventListener {

    /**
     * Constructor.
     */
    public ProcedureInstanceCreatedListener() {
        super();
    }

    /**
     * Set Read permission on ProcedureInstance for Procedure initiator.
     */
    @Override
    public void handleEvent(Event event) {
        // It is necessary a DocumentEventContext (cf instances/listener-contrib.xml).
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        DocumentModel sourceDocument = docCtx.getSourceDocument();

        if(ProceduresConstants.PI_TYPE.equals(sourceDocument.getType())
                && DocumentEventTypes.DOCUMENT_CREATED.equals(event.getName())){

            NuxeoPrincipal principal = (NuxeoPrincipal) docCtx.getPrincipal();
            ToutaticeDocumentHelper.setACE(docCtx.getCoreSession(), sourceDocument.getRef(),
                    new ACE(principal.getActingUser(), SecurityConstants.READ, true));

            try {
				ToutaticeDocumentHelper.saveDocumentSilently(docCtx.getCoreSession(), sourceDocument, true);
			} catch (LoginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

}
