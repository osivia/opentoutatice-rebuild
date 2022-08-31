package fr.toutatice.ecm.platform.core.services.infos.provider;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class DocumentInformationsProviderServiceImpl extends DefaultComponent implements DocumentInformationsProviderService {

    private static final long serialVersionUID = 3212642773317224973L;
    
    private static final Log log = LogFactory.getLog(DocumentInformationsProviderServiceImpl.class);
    
    /** Fetch infos extension point. */
    public static final String FETCH_INFOS_EXT_POINT = "fetch_infos";
    /** Extended fetch infos extension point. */
    public static final String EXTENDED_FETCH_INFOS_EXT_POINT = "extended_fetch_infos";

    private Map<String, DocumentInformationsProvider> infosProvidersRegistry;
    private Map<String, DocumentInformationsProvider> extendedInfosProvidersRegistry;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        infosProvidersRegistry = new HashMap<String, DocumentInformationsProvider>(0);
        extendedInfosProvidersRegistry = new HashMap<String, DocumentInformationsProvider>(0);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void registerExtension(Extension extension) {
        if(FETCH_INFOS_EXT_POINT.equals(extension.getExtensionPoint())){
            registerInfosProvider(extension, infosProvidersRegistry);
        } else if(EXTENDED_FETCH_INFOS_EXT_POINT.equals(extension.getExtensionPoint())){
            registerInfosProvider(extension, extendedInfosProvidersRegistry);
        }
    }
    
    /**
     * Register a given provider identified by its name.
     * New provider with same name will be override.
     * 
     * @param extension
     * @param registeredInfosProviders
     * @throws Exception
     */
    private void registerInfosProvider(Extension extension, Map<String, DocumentInformationsProvider> registeredInfosProviders)  {
        Object[] contributions = extension.getContributions();
        for (Object contribution : contributions) {
            if(contribution instanceof DocumentInformationsProviderDescriptor){
                DocumentInformationsProviderDescriptor descriptor = (DocumentInformationsProviderDescriptor) contribution;
                if(StringUtils.isNotBlank(descriptor.getName())){
                    DocumentInformationsProvider infosProvider = null;
                    try {
                        infosProvider = (DocumentInformationsProvider) extension.getContext().loadClass(descriptor.getProviderClassName()).newInstance();
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    registeredInfosProviders.put(descriptor.getName(), infosProvider);
                } else {
                    log.error("Can not register an Informations provider which has no name: "
                            .concat(StringUtils.isNotBlank(descriptor.getProviderClassName()) ? descriptor.getProviderClassName() : "null"));
                }
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterExtension(Extension extension)  {
        if(FETCH_INFOS_EXT_POINT.equals(extension.getExtensionPoint())){
            unregisterInfosProvider(extension, infosProvidersRegistry);
        } else if(EXTENDED_FETCH_INFOS_EXT_POINT.equals(extension.getExtensionPoint())){
            unregisterInfosProvider(extension, extendedInfosProvidersRegistry);
        }
    }
    
    /**
     * Unregister a provider.
     * 
     * @param extension
     * @param registeredInfosProviders
     */
    private void unregisterInfosProvider(Extension extension, Map<String, DocumentInformationsProvider> registeredInfosProviders) {
        Object[] contributions = extension.getContributions();
        for (Object contribution : contributions) {
            if(contribution instanceof DocumentInformationsProviderDescriptor){
                registeredInfosProviders.remove(((DocumentInformationsProviderDescriptor) contribution).getName());
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> fetchAllInfos(CoreSession coreSession, DocumentModel currentDocument) {
        Map<String, Object> infos = new HashMap<String, Object>(0);
        for (DocumentInformationsProvider contrib : infosProvidersRegistry.values()) {
            infos.putAll(contrib.fetchInfos(coreSession, currentDocument));
        }
        return infos;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> fetchAllExtendedInfos(CoreSession coreSession, DocumentModel currentDocument) {
        Map<String, Object> infos = new HashMap<String, Object>(0);
        for (DocumentInformationsProvider contrib : extendedInfosProvidersRegistry.values()) {
            // For trace logs
            long begin = System.currentTimeMillis();
            
            infos.putAll(contrib.fetchInfos(coreSession, currentDocument));
            
            if(log.isTraceEnabled()){
                long end = System.currentTimeMillis();
                log.trace(" " + contrib.getClass().getName() + ": " + String.valueOf(end - begin) + " ms");
            }
        }
        return infos;
    }
}
