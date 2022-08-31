package fr.toutatice.ecm.platform.core.services.infos.provider;

import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface DocumentInformationsProvider {
    
    /**
     * Fetch provider's informations on current document.
     * 
     * @param coreSession
     * @param currentDocument
     * @return current document'informations for provier.

     */
    Map<String, Object> fetchInfos(CoreSession coreSession, DocumentModel currentDocument);

}
