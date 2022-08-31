package fr.toutatice.ecm.platform.core.services.infos.provider;


import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

@XObject("fetchInfos")
public class DocumentInformationsProviderDescriptor implements Serializable {

	private static final long serialVersionUID = -2001913048962270285L;
	
	@XNode("@name")
	private String name;

    @XNode("@class")
    private String providerClassName;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the providerClassName
     */
    public String getProviderClassName() {
        return providerClassName;
    }
    
    /**
     * @param providerClassName the providerClassName to set
     */
    public void setProviderClassName(String providerClassName) {
        this.providerClassName = providerClassName;
    }
    
    

}
