/**
 * 
 */
package org.osivia.procedures.instances.security;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.REMOVE_CHILDREN;
import static org.osivia.procedures.constants.ProceduresConstants.PI_CONTAINER_TYPE;

import java.security.Principal;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.security.AbstractSecurityPolicy;


/**
 * @author david
 *
 */
public class ContainerSecurityPolicy extends AbstractSecurityPolicy {

    /**
     * Constructor.
     */
    public ContainerSecurityPolicy() {
        super();
    }



    @Override
    public boolean isRestrictingPermission(String permission) {
        return true;
    }

    @Override
    public boolean isExpressibleInQuery(String repositoryName) {
        return true;
    }

    @Override
    public SQLQuery.Transformer getQueryTransformer(String repositoryName) {
        return SQLQuery.Transformer.IDENTITY;
    }
    
    /**
     * Shortcuts REMOVE_CHILDREN permission test on ProcedureInstances
     * container when want to delete one ProcedureInstance.
     */
	@Override
	public Access checkPermission(Document doc, ACP mergedAcp, NuxeoPrincipal principal, String permission,
			String[] resolvedPermissions, String[] additionalPrincipals) {
        if(REMOVE_CHILDREN.equals(permission)
                && PI_CONTAINER_TYPE.equals(doc.getType().getName())){
            // At this point, we have REMOVE permission on procedureInstance
            // (cf AbstractSession#canRemoveDocument)
            return Access.GRANT;
        }
        return Access.UNKNOWN;
	}

}
