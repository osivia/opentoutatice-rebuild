/**
 * 
 */
package fr.toutatice.ecm.platform.automation.trash;


/**
 * @author david
 *
 */
public interface TrashHelper {
    
    /** Trash request. */
    String TRASH_REQUEST = "select * from Document where ecm:ancestorId = '%s' and ecm:currentLifeCycleState = 'deleted' "
            + " and ecm:isProxy = 0 and ecm:isVersion = 0";

}
