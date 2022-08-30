/**
 * 
 */
package fr.toutatice.ecm.platform.automation.security;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.security.MasterOwnerSecurityHelper;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;


/**
 * @author david
 *
 */
public class ACEsOperationHelper {
    
    /**
     * Utility class.
     */
    private ACEsOperationHelper(){};
    
    /**
     * Getter for block inheritance ACE.
     * 
     * @return block inheritance ACE
     */
    public static ACE getBlockInheritanceACe(){
        return new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
    }
    
    /**
     * Build list of ACE objects from Properties parameter
     * (as negative ACEs are not allowed, they are all set to granted: true).
     * 
     * @param aces
     * @return list of ACE objects
     * @throws TypeAdaptException 
     */
    public static List<ACE> buildACEs(OperationContext ctx, Properties aces) throws TypeAdaptException{
        List<ACE> aceEntries = new ArrayList<ACE>(0);
        
        if(aces != null){
            for(Entry<String, String> aceProp : aces.entrySet()){
                // Value is a String of the from: [a, b, ...]
                String permsAsString = StringUtils.substringBetween(aceProp.getValue(), "[", "]");
                if(permsAsString != null){
                String[] permissions = permsAsString.split(",");
                
                // Add unitary permissions
                if(permissions != null){
                    for(String permission : permissions){
                        ACE aceEntry = new ACE(aceProp.getKey(), StringUtils.trimToEmpty(permission));
                        aceEntries.add(aceEntry);
                    }
                }
            }
            }
        } 
        
        return aceEntries;
    }
    
    /**
     * Gets default local ACL, i.e. when inheritance
     * is blocked.
     * 
     * @return default local ACL
     */
    public static ACL buildDefaultLocalACL(CoreSession session, DocumentModel document) {
        ACL acl = new ACLImpl();

        String currentUser = session.getPrincipal().getName();
        acl.add(new ACE(currentUser, SecurityConstants.EVERYTHING));

        // acl.addAll(ACEsOperationHelper.getAdminEverythingACEs());
        acl.addAll(getMasterOwnerACEs(session, document));

        return acl;
    }
    
    /**
     * Return a list of ACE giving everything permission to admin groups.
     *
     * @return list of ACE
     */
    public static List<ACE> getAdminEverythingACEs() {
        List<ACE> result = new ArrayList<>();
        UserManager um = Framework.getLocalService(UserManager.class);
        List<String> administratorsGroups = um.getAdministratorsGroups();
        for (String adminGroup : administratorsGroups) {
            result.add(new ACE(adminGroup, SecurityConstants.EVERYTHING, true));
        }
        return result;

    }
    
    /**
     * Gets list of Master Owners of document.
     * 
     * @return list of Master Owners of document
     */
    public static List<ACE> getMasterOwnerACEs(CoreSession session, DocumentModel document){
        List<ACE> acEs = new ArrayList<ACE>(0);
        
        List<String> masterOwners = MasterOwnerSecurityHelper.getMasterOwners(session, document);
        for (String masterOwner : masterOwners){
            ACE ace = new ACE(masterOwner, ToutaticeNuxeoStudioConst.CST_PERM_MASTER_OWNER);
            acEs.add(ace);
        }
        
        return acEs;
    }
    
}
