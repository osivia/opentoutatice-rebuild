/**
 * 
 */
package fr.toutatice.ecm.es.customizer.registry;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;


/**
 * @author david
 *
 */
@XObject("listener")
public class ESListenerDescriptor {
    
    @XNode("@class")
    private String clazz;
    
    public String getClazz(){
        return this.clazz;
    }
    
    @XNode("@order")
    private int order = 0;
    
    public int getOrder(){
        return this.order;
    }
    
    @XNode("@enabled")
    private boolean enabled = true;
    
    public boolean isEnabled(){
        return this.enabled;
    }
    
}
