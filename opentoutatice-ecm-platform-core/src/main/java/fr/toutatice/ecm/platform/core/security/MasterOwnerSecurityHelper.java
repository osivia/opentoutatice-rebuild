/**
 * 
 */
package fr.toutatice.ecm.platform.core.security;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;

import java.util.ArrayList;
import java.util.List;


/**
 * @author david
 *
 */
public class MasterOwnerSecurityHelper {

    /**
     * Utility class.
     */
    private MasterOwnerSecurityHelper() {
        super();
    }
    
    /**
     * Gets Master Owners of document (users and groups).
     * 
     * @param session
     * @param document
     * @return list of Master Owners
     */
    public static List<String> getMasterOwners (CoreSession session, DocumentModel document) {
        List<String> masterOwners = new ArrayList<String>(0);
        
        DocumentModel workspace = ToutaticeDocumentHelper.getWorkspace(session, document, true);
        
        if(workspace != null){
            ACP acp = workspace.getACP();
            ACL[] acLs = acp.getACLs();
            
            for(ACL acl : acLs){
                for(ACE ace :acl){
                    if(ToutaticeNuxeoStudioConst.CST_PERM_MASTER_OWNER.equals(ace.getPermission())){
                        masterOwners.add(ace.getUsername());
                    }
                }
            }
            
        }
        
        return masterOwners;
    }

}
