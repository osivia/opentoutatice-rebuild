package fr.toutatice.ecm.platform.core.services.infos.provider;

import java.io.Serializable;
import java.util.Map;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface DocumentInformationsProviderService extends Serializable {
    
    /**
     * Fetch informations's document for all registered providers.
     * 
     * @param coreSession
     * @param currentDocument
     * @return informations's document for all registered providers.
     */
	Map<String, Object> fetchAllInfos(CoreSession coreSession,
			DocumentModel currentDocument);
	
	/**
	 * Fetch extended informations's document for all registered providers.
	 * 
	 * @param coreSession
	 * @param currentDocument
	 * @return extended informations's document for all registered providers.
	 */
	Map<String, Object> fetchAllExtendedInfos(CoreSession coreSession,
            DocumentModel currentDocument);

}
