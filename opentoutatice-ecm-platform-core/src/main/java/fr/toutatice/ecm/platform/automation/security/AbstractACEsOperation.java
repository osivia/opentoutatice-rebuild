/**
 * 
 */
package fr.toutatice.ecm.platform.automation.security;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;

import java.util.List;


/**
 * Abstract class to add or remove ACEs in inherited or local ACL.
 * It does not allow possibility to add / remove ACEs in inherited ACL
 * if blockInhertance is set to true.
 * 
 * @author david
 *
 */
public abstract class AbstractACEsOperation {

    /**
     * Constructor.
     */
    public AbstractACEsOperation() {
        super();
    }

    /**
     * Adds or remove ACEs from document.
     * 
     * @param document
     * @return document
     * @throws Exception
     */
    protected DocumentModel execute(OperationContext ctx, CoreSession session, DocumentModel document, String aclName, Properties aces, boolean blockInheritance) throws Exception {
     // Convert ACES
        List<ACE> inputACEs = ACEsOperationHelper.buildACEs(ctx, aces);

        // ACP
        ACP acp = document.getACP();

        // Local ACL
        if (ACL.LOCAL_ACL.equals(aclName)) {
            // Local ACL
            ACL localAcl = acp.getOrCreateACL(ACL.LOCAL_ACL);
            
            // Block inheritance
            if (blockInheritance) {
                // Blocked ACL
                localAcl = blockLocalACLIfNecessary(session, document, localAcl);

                // Modify local ACL with given ACEs
                localAcl = modifyACEs(localAcl, inputACEs);

                // To clear caches
                acp.addACL(localAcl);
            } else {
                // Inheritance
                acp = restoreInheritanceIfNecessary(session, document, localAcl);

                // Modify
                localAcl = modifyACEs(acp.getACL(ACL.LOCAL_ACL), inputACEs);

                // To clear caches
                acp.addACL(localAcl);
            }

        } else {
            if (ACL.INHERITED_ACL.equals(aclName)) {
                throw new Exception("You can not alter inherited ACL.");
            }

            ACL acl = acp.getOrCreateACL(aclName);
            // Modify
            acl = modifyACEs(acl, inputACEs);
        }

        // Save
        document.setACP(acp, true);

        return document;
    }

    /**
     * Removes all ACEs of ACL.
     * 
     * @param session
     * @param document
     * @param aclName
     * @return document
     */
    protected DocumentModel execute(CoreSession session, DocumentModel document, String aclName) throws Exception {
        ACP acp = document.getACP();
        acp.removeACL(aclName);
        document.setACP(acp, true);
        return document;
    }

    /**
     * Adds or removes ACEs in given ACL.
     * 
     * @param acl
     * @param aces
     * @return modified acl
     */
    protected abstract ACL modifyACEs(ACL acl, List<ACE> aces);

    /**
     * Blocks inheritance and set default rule.
     * 
     * @param session
     * @param document
     * @return acl
     */
    protected ACL blockLocalACLIfNecessary(CoreSession session, DocumentModel document, ACL localAcl) {
        // Block ACL
        ACE blockInhACe = ACEsOperationHelper.getBlockInheritanceACe();

        if (!localAcl.contains(blockInhACe)) {
            // Add default rule
            ACL defaultLocalACL = ACEsOperationHelper.buildDefaultLocalACL(session, document);
            for(ACE ace : defaultLocalACL){
                if(!localAcl.contains(ace)){
                    localAcl.add(ace);
                }
            }

            // Blocks
            localAcl.add(blockInhACe);
        }

        return localAcl;
    }

    /**
     * Restore inheritance.
     * 
     * @param session
     * @param document
     * @return acp
     */
    protected ACP restoreInheritanceIfNecessary(CoreSession session, DocumentModel document, ACL localAcl) {
        // ACP
        ACP acp = document.getACP();

        // Remove default rule
        ACL defaultLocalACL = ACEsOperationHelper.buildDefaultLocalACL(session, document);
        if (localAcl.containsAll(defaultLocalACL)) {
            localAcl.removeAll(defaultLocalACL);
        }

        // Remove block to restore inheritance
        ACE blockInACe = ACEsOperationHelper.getBlockInheritanceACe();
        if (localAcl.contains(blockInACe)) {
            localAcl.remove(blockInACe);
        }

        // To clear cache
        acp.addACL(localAcl);

        return acp;
    }
//
//    protected ACP removeACEs(CoreSession session, DocumentModel document, List<ACE> aces) {
//        // ACP
//        ACP acp = document.getACP();
//        // Get local ACL
//        ACL localAcl = acp.getACL(ACL.LOCAL_ACL);
//
//        // Add
//        for (ACE aceToRemove : aces) {
//            if (localAcl.contains(aceToRemove)) {
//                localAcl.remove(aceToRemove);
//            }
//        }
//
//        // To clear cache
//        acp.addACL(localAcl);
//
//        return acp;
//    }

}
