/**
 * 
 */
package fr.toutatice.ecm.platform.automation.security;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.platform.usermanager.UserManager;


/**
 * @author david
 *
 */
@Operation(id = GetDocumentACLs.ID, category = Constants.CAT_DOCUMENT, label = "Gets document's ACLs", description = "Gets document's ACLs")
public class GetDocumentACLs {
    
    /** Operation's id. */
    public final static String ID = "Document.GetACLs";
    
    /** Session. */
    @Context
    protected CoreSession session;
    
    /** User Manager. */
    @Context
    protected UserManager userManager;
    
    /**
     * @param document
     * @return ACEs of document as List.
     */
    @OperationMethod
    public Object run(DocumentModel document) throws Exception {
        JSONObject allACLs = new JSONObject();
        
        ACP acp = session.getACP(document.getRef());
        if (acp != null) {
            ACL[] acLs = acp.getACLs();
            if (ArrayUtils.isNotEmpty(acLs)) {
                JSONArray inheritedACLs = new JSONArray();
                JSONArray localACLs = new JSONArray();
                for (ACL acl : acLs) {
                    if (ACL.INHERITED_ACL.equals(acl.getName())) {
                        extractNSetACEs(inheritedACLs, acl);
                    } else if (ACL.LOCAL_ACL.equals(acl.getName())) {
                        extractNSetACEs(localACLs, acl);
                    }
                }
                allACLs.put(ACL.INHERITED_ACL, inheritedACLs);
                allACLs.put(ACL.LOCAL_ACL, localACLs);
            }
        }
        
        return new StringBlob(allACLs.toString(), "application/json");
    }

    /**
     * Extract ACEs of given ACL and set them in JSONArray.
     * 
     * @param jsonACEs
     * @param acl
     */
    protected void extractNSetACEs(JSONArray jsonACEs, ACL acl) throws JSONException {
        ACE[] acEs = acl.getACEs();
        if (ArrayUtils.isNotEmpty(acEs)) {
            //List<String> groupIds = userManager.getGroupIds();
            for (ACE ace : acEs) {
                jsonACEs.put(convert(ace));
            }
        }
    }
    
    /**
     * Converts ACE to JSOObject.
     * 
     * @param ace
     * @return ACE as JSONObject
     */
    protected JSONObject convert(ACE ace) throws JSONException {
        JSONObject aceEntry = new JSONObject();
        aceEntry.put("username", ace.getUsername());
        aceEntry.put("permission", ace.getPermission());
        aceEntry.put("isGranted", ace.isGranted());
        
//        if(CollectionUtils.isNotEmpty(groupIds)){
        
        // #1940 -read acl performance
    	NuxeoGroup group = userManager.getGroup(ace.getUsername());
        aceEntry.put("isGroup", group != null);
            
//        } else {
//            aceEntry.element("isGroup", false);
//        }
        
        return aceEntry;
    }

}
