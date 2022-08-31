/*
 * (C) Copyright 2014 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * 
 * Contributors:
 * mberhaut1
 * lbillon
 * dchevrier
 */
package fr.toutatice.ecm.platform.automation;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.toutatice.ecm.platform.service.url.NoSuchDocumentException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.impl.primitives.BooleanProperty;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.platform.core.constants.ToutaticeGlobalConst;
import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProviderService;
import fr.toutatice.ecm.platform.service.url.WebIdResolver;

@Operation(id = FetchPublicationInfos.ID, category = Constants.CAT_FETCH, label = "Fetch publish space informations",
        description = "Fetch informations about the publish space, worksapce, proxy status, ... of a given document.")
public class FetchPublicationInfos {

    private static final Log log = LogFactory.getLog(FetchPublicationInfos.class);

    /**
     * Id Nuxeo de l'opération (s'applique à un Document).
     */
    public static final String ID = "Document.FetchPublicationInfos";

    /**
     * Codes d'erreur
     */
    public static final int ERROR_CONTENT_NOT_FOUND = 1;
    public static final int ERROR_CONTENT_FORBIDDEN = 2;
    public static final int ERROR_PUBLISH_SPACE_NOT_FOUND = 3;
    public static final int ERROR_PUBLISH_SPACE_FORBIDDEN = 4;
    public static final int ERROR_WORKSPACE_NOT_FOUND = 5;
    public static final int ERROR_WORKSPACE_FORBIDDEN = 6;
    public static final int SERVER_ERROR = 500;
    public static final String INTERNAL_PROCESSING_ERROR_RESPONSE = "InternalProcessingErrorResponse";
    private static final String TOUTATICE_PUBLI_SUFFIX = ".proxy";

    /**
     * Suufixe du nom des proxies.
     */
    private static final String SUFFIXE_PROXY = ".proxy";
    /**
     * Propriété de contextualisation (schéma toutatice).
     */
    private static final String IN_CONTEXTUALIZATON_PROPERTY = "ttc:contextualizeInternalContents";

    /**
     * Session.
     */
    @Context
    protected CoreSession coreSession;

    /**
     * Service gérant les types.
     */
    @Context
    protected TypeManager typeService;

    /**
     * Service gérant les utilisateurs.
     */
    @Context
    protected UserManager userManager;

    /**
     * Identifiant ("path" ou uuid) du document en entrée.
     */
    @Param(name = "path", required = false)
    protected String path;
    //protected DocumentModel document;

    @Param(name = "webid", required = false)
    protected String webid;

    
	private DocumentRef docRef;

    @OperationMethod
    public Object run() throws Exception {
        // For Trace logs
        long begin = System.currentTimeMillis();
        if(log.isTraceEnabled()){
            String id = this.path == null ? this.webid : this.path;
            log.trace(" ID: " + id);
        }

        /* Réponse de l'opération sous forme de flux JSon */
        JSONArray rowInfosPubli = new JSONArray();
        JSONObject infosPubli = new JSONObject();

        List<Integer> errorsCodes = new ArrayList<Integer>();

        // WebId given
        if (StringUtils.isNotBlank(webid)) {
            
        	// LBI #1804 - change input from documentModel to String to avoid long stacktraces 
        	try {
        		path = getDocumentPathByWebId(webid);
        	}
        	catch(NoSuchDocumentException e) {
        		errorsCodes.add(ERROR_CONTENT_NOT_FOUND);
                infosPubli.element("errorCodes", errorsCodes);
                rowInfosPubli.add(infosPubli);
                return createBlob(rowInfosPubli);
        	}
        }
        if(path.startsWith("/")) {
        	docRef = new PathRef(path);
        }
        else {
        	docRef = new IdRef(path);
        }
        
        
        Object fetchDocumentRes = getDocument(docRef);
        /*
         * Chaque méthode "principale utilisée peut retourner un objet de type
         * Boolean ou de type DocumentModel ou d'un autre type qui est alors
         * considéré comme une erreur
         */
        if (isError(fetchDocumentRes)) {
            errorsCodes.add((Integer) fetchDocumentRes);
            infosPubli.element("errorCodes", errorsCodes);
            rowInfosPubli.add(infosPubli);
            return createBlob(rowInfosPubli);
        }
        DocumentModel document = (DocumentModel) fetchDocumentRes;
        
        // Version
        boolean isVersion = document.isVersion();
        infosPubli.element("isVersion", isVersion);

        /*
         * Test du droit de modification, suppression sur le document.
         */
        Object liveDocRes = getLiveDoc(coreSession, document, infosPubli);
        if (isError(liveDocRes)) {
            infosPubli = (JSONObject) liveDocRes;
            infosPubli.element("documentPath", URLEncoder.encode(document.getPath().toString(), "UTF-8"));
            infosPubli.element("liveId", StringUtils.EMPTY);
            infosPubli.element("editableByUser", Boolean.FALSE);
            infosPubli.element("isDeletableByUser", Boolean.FALSE);
            
            // #1782 Cas de la dépublication sans avoir de live, contrôle des droits sur le proxy
            Boolean isEditable = isEditableByUser(infosPubli, document);
            Object canUnpublishRemoteProxy = canUnpublishRemoteProxy(isEditable, document);
            infosPubli.element("canUnpublishRemoteProxy", canUnpublishRemoteProxy);
            
        } else {
            DocumentModel liveDoc = (DocumentModel) liveDocRes;
            infosPubli.element("liveId", liveDoc.getId());
            
            // #1782 Cas de la dépublication avec un live en corbeille
            if(ToutaticeGlobalConst.CST_DOC_STATE_DELETED.equals(liveDoc.getCurrentLifeCycleState())) {
                infosPubli.element("isLiveDeleted", Boolean.TRUE);
            }
            
            Boolean isEditable = isEditableByUser(infosPubli, liveDoc);
            infosPubli.element("editableByUser", isEditable);
            Object isManageable = isManageableByUser(infosPubli, liveDoc);
            infosPubli.element("manageableByUser", isManageable);
            Object isDeletable = isDeletableByUser(infosPubli, liveDoc);
            infosPubli.element("isDeletableByUser", isDeletable);

            Object canUserValidate = canUserValidate();
            infosPubli.element("canUserValidate", canUserValidate);
            
            // LBI #1780 - droit à la dépublication
            Object canUnpublishRemoteProxy = canUnpublishRemoteProxy(isEditable, document);
            infosPubli.element("canUnpublishRemoteProxy", canUnpublishRemoteProxy);
            
            
            /*
             * Récupération du path du document - cas où un uuid ou un webId est donné en
             * entrée
             */
            String livePath = liveDoc.getPathAsString();
            String docPath = document.getPath().toString();
            String path = docPath;
            if (docPath.endsWith(TOUTATICE_PUBLI_SUFFIX) && docPath.equals(livePath + TOUTATICE_PUBLI_SUFFIX)) {
                path = livePath;
            }
            infosPubli.element("documentPath", URLEncoder.encode(path, "UTF-8"));

            /* Indique une modification du live depuis la dernière publication du proxy */
            liveDoc = (DocumentModel) liveDocRes;
            infosPubli.element("liveVersion", liveDoc.getVersionLabel());


            /*
             * Extended informations
             */
            DocumentInformationsProviderService fetchInfosService = Framework.getService(DocumentInformationsProviderService.class);
            if (fetchInfosService != null) {
                Map<String, Object> infosSynchro = fetchInfosService.fetchAllInfos(coreSession, liveDoc);
                infosPubli.accumulateAll(infosSynchro);
            }

        }

        infosPubli.put("subTypes", new JSONObject());
        if (document.isFolder()) {
            infosPubli.put("subTypes", getSubTypes(coreSession, document));
        }


        /*
         * Récupération du "droit" de commenter.
         */
        boolean docCommentable = document.hasFacet("Commentable");
        Principal user = coreSession.getPrincipal();
/*        if (user == null) {
            throw new ClientException("Current user not found.");
        }*/
        boolean userNotAnonymous = !((NuxeoPrincipal) user).isAnonymous();
        
        if (document.hasSchema(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE) && docCommentable) {
	        // #1444 Un document peut être unitairement interdit de commentaire
	        Serializable commentsForbidden = document.getPropertyValue(ToutaticeNuxeoStudioConst.TTC_COMMENTS_FORBIDDEN);
	        if(commentsForbidden != null && ((Boolean)commentsForbidden == true)) {
	        	docCommentable = false;
	        }
        }
        
        infosPubli.put("isCommentableByUser", docCommentable && userNotAnonymous);

        if (log.isTraceEnabled()) {
            log.trace(" [Before UnrestrictedFecthPubliInfosRunner]: " + String.valueOf(System.currentTimeMillis() - begin));
        }

        UnrestrictedFecthPubliInfosRunner infosPubliRunner = new UnrestrictedFecthPubliInfosRunner(coreSession, document, liveDocRes, infosPubli, userManager,
                errorsCodes);

        infosPubliRunner.runUnrestricted();
        errorsCodes = infosPubliRunner.getErrorsCodes();
        infosPubli = infosPubliRunner.getInfosPubli();
        infosPubli.element("errorCodes", errorsCodes);
        rowInfosPubli.add(infosPubli);
        
        if(log.isTraceEnabled()){
            long end = System.currentTimeMillis();
            log.trace(" Ended: " + String.valueOf(end - begin) + " ms ======= \r\n");
        }

        return createBlob(rowInfosPubli);
    }



	/**
     * Gets allowed subTypes for given folder.
     * 
     */
    public JSONObject getSubTypes(CoreSession session, DocumentModel folder) throws UnsupportedEncodingException {
        JSONObject subTypes = new JSONObject();

        boolean canAddChildren = session.hasPermission(folder.getRef(), SecurityConstants.ADD_CHILDREN);
        if (canAddChildren) {
            Collection<Type> allowedSubTypes = this.typeService.getAllowedSubTypes(folder.getType());
            for (Type subType : allowedSubTypes) {
                subTypes.put(subType.getId(), URLEncoder.encode(subType.getLabel(), "UTF-8"));
            }
        }

        return subTypes;
    }

    /**
     * Récupère un code d'erreur.
     *
     * @param errorCodeNotFound
     *            code d'erreur si un document n'est pas trouvé
     * @param errorCodeForbidden
     *            code d'erreur si l'utilisateur n'a pas le droit de lecture sur
     *            un document
     * @return le code d'erreur correspondant à ceux présents dans l'en-tête de
     *         operationRes
     */
    private static int getErrorCode(Exception inputException, int errorCodeNotFound, int errorCodeForbidden) {
        Exception exception = inputException;
        int errorCode = 0;
        if (exception instanceof NoSuchDocumentException) {
            errorCode = errorCodeNotFound;
        } else if (exception instanceof DocumentSecurityException) {
            errorCode = errorCodeForbidden;
        }
        return errorCode;
    }

    /**
     * Méthode permettant de vérifier si un document (live) est modifiable par
     * l'utilisateur.
     * 
     * @param infos
     *            pour stocker le résultat du test (booléen)
     * @param liveDoc
     *            document testé
     * @return vrai si le document est modifiable par l'utilisateur
     * @throws ServeurException
     */
    private Boolean isEditableByUser(JSONObject infos, DocumentModel liveDoc) throws ServeurException {
        Boolean canModify = null;
/*        try {*/
            canModify = Boolean.valueOf(coreSession.hasPermission(liveDoc.getRef(), SecurityConstants.WRITE));
/*        } catch (ClientException e) {
            if (e instanceof DocumentSecurityException) {
                return Boolean.FALSE;
            } else {
                log.warn("Failed to fetch permissions for document '" + liveDoc.getPathAsString() + "', error:" + e.getMessage());
                throw new ServeurException(e);
            }
        }*/
        return canModify;
    }

    /**
     * Méthode permettant de vérifier si un document (live) est gérable par
     * l'utilisateur.
     * 
     * @param infos
     *            pour stocker le résultat du test (booléen)
     * @param liveDoc
     *            document testé
     * @return vrai si le document est gérable par l'utilisateur
     * @throws ServeurException
     */
    private Object isManageableByUser(JSONObject infos, DocumentModel liveDoc)  {
        Boolean canManage = null;
/*        try {*/
            canManage = Boolean.valueOf(coreSession.hasPermission(liveDoc.getRef(), SecurityConstants.EVERYTHING));
/*        } catch (ClientException e) {
            if (e instanceof DocumentSecurityException) {
                return Boolean.FALSE;
            } else {
                log.warn("Failed to fetch permissions for document '" + liveDoc.getPathAsString() + "', error:" + e.getMessage());
                throw new ServeurException(e);
            }
        }*/
        return canManage;
    }

    /**
     * Get user validate rigth on document.
     * 
     * @throws ServeurException
     */
    private Boolean canUserValidate() {
        // Direct on/off line
        return checkValidatePermission();
    }

    private Boolean checkValidatePermission()  {
        Boolean canValidate = Boolean.FALSE;
/*        try {*/
            
			canValidate = Boolean.valueOf(coreSession.hasPermission(docRef, ToutaticeNuxeoStudioConst.CST_PERM_VALIDATE));
/*        } catch (ClientException e) {
            if (e instanceof DocumentSecurityException) {
                return Boolean.FALSE;
            } else {
                log.warn("Failed to fetch permissions for document '" + path + "', error:" + e.getMessage());
                throw new ServeurException(e);
            }
        }*/
        return canValidate;
    }
    
    private Boolean canUnpublishRemoteProxy(Boolean isEditable, DocumentModel document)  {
    	Boolean canUnpublishRemoteProxy = Boolean.FALSE;
    	
		if(document.hasFacet("isRemoteProxy") && isEditable) {
	        /*
	        try {*/
	        	canUnpublishRemoteProxy = Boolean.valueOf(coreSession.hasPermission(document.getRef(), ToutaticeNuxeoStudioConst.CST_PERM_REMOTE_PUBLISH));
/*	        } catch (ClientException e) {
	            if (e instanceof DocumentSecurityException) {
	                return Boolean.FALSE;
	            } else {
	                log.warn("Failed to fetch permissions for document '" + document.getPathAsString() + "', error:" + e.getMessage());
	                throw new ServeurException(e);
	            }
	        }*/

		}

		return canUnpublishRemoteProxy;
	}

    /**
     * Méthode permettant de vérifier si un document (live) est supprimable par
     * l'utilisateur.
     * 
     * @param infos
     *            pour stocker le résultat du test (booléen)
     * @param liveDoc
     *            document testé
     * @return vrai si le document est supprimable par l'utilisateur
     * @throws Exception
     */
    private Object isDeletableByUser(JSONObject infos, DocumentModel liveDoc) throws Exception {
        Boolean canBeDelete = Boolean.FALSE;
        /*try {*/
            TrashService trash = Framework.getService(TrashService.class);
            List<DocumentModel> docs = new ArrayList<DocumentModel>();
            docs.add(liveDoc);
            canBeDelete = trash.canDelete(docs, coreSession.getPrincipal(), false);

            /*
             * Règle de gestion liée au droit de validation et à l'existence de proxy local:
             * Un document dans l'état validé ou bien qui est publié peut être supprimé seulement
             * si l'usager connecté possède le droit de validation.
             */
            if (canBeDelete) {
                DocumentModel proxy = ToutaticeDocumentHelper.getProxy(coreSession, liveDoc, null);
                boolean hasProxy = (null != proxy);
                /* 3.0 Report: boolean isApproved = ToutaticeNuxeoStudioConst.CST_DOC_STATE_APPROVED.equals(liveDoc.getCurrentLifeCycleState());*/
                if (/* isApproved || */hasProxy) {
                    boolean canValidate = coreSession.hasPermission(liveDoc.getRef(), ToutaticeNuxeoStudioConst.CST_PERM_VALIDATE);
                    canBeDelete = Boolean.valueOf(canValidate);
                }
            }
/*        } catch (ClientException e) {
            if (e instanceof DocumentSecurityException) {
                return Boolean.FALSE;
            } else {
                log.warn("Failed to fetch permissions for document '" + liveDoc.getPathAsString() + "', error:" + e.getMessage());
                throw new ServeurException(e);
            }
        }*/

        return canBeDelete;
    }


    /**
     * Méthode permettant de "fetcher" un document live et mettant à faux le
     * booléen editableByUser en cas d'erreur.
     * 
     * @param session
     *            session Nuxeo
     * @param doc
     *            document dont on cherche la version live
     * @param infos
     *            permet de stocker le booléen editableByUser
     * @return la version live ou l'objet infos avec la propriété editableByUser
     *         mise à faux en cas d'erreur à la récupération de la version live
     * @throws ServeurException
     */
    private Object getLiveDoc(CoreSession session, DocumentModel doc, JSONObject infos) {
        DocumentModel liveDoc = null;
        /*try {*/
            DocumentModel srcDocument = session.getSourceDocument(doc.getRef());
            if (session.hasPermission(srcDocument.getRef(), SecurityConstants.READ_VERSION)) {
                liveDoc = session.getWorkingCopy(srcDocument.getRef());
            }
/*        } catch (ClientException ce) {
            if (ce instanceof DocumentSecurityException) {
                infos.element("editableByUser", Boolean.FALSE);
                return infos;
            } else {
                log.warn("Failed to fetch live document of document'" + doc.getPathAsString() + "', error:" + ce.getMessage());
                throw new ServeurException(ce);
            }
        }*/
        if (liveDoc == null) {
            infos.element("editableByUser", Boolean.FALSE);
            return infos;
        }
        return liveDoc;
    }

    /**
     * Méthode permettant de récupérer un document suivant sa référence; stocke,
     * le cas échéant, les erreurs 401 ou 404.
     * 
     * @param refDoc
     *            référence du document
     * @return un DocumentModel ou l'objet erros en cas d'erreur à la
     *         récupération
     * @throws ServeurException
     */
    private Object getDocument(DocumentRef refDoc)  {
        DocumentModel doc = null;
/*        try {*/
            doc = coreSession.getDocument(refDoc);
/*        } catch (ClientException ce) {
            if (ce instanceof DocumentSecurityException) {
                return ERROR_CONTENT_FORBIDDEN;
            } else {
                if (isNoSuchDocumentException(ce)) {
                    return ERROR_CONTENT_NOT_FOUND;
                } else {
                    log.warn("Failed to fetch document with path or uid: '" + path + "', error:" + ce.getMessage());
                    throw new ServeurException(ce);
                }
            }
        }*/
        if (doc == null) {
            return ERROR_CONTENT_NOT_FOUND;
        }
        return doc;
    }
    
    /**
     * @param webid
     * @return document if found, error otherwise.
     * @throws NoSuchDocumentException 
     * @throws ServeurException
     */
    private String getDocumentPathByWebId(String webid) throws NoSuchDocumentException {
        // Trace logs
        long begin = System.currentTimeMillis();
        
        DocumentModel doc = null;

        DocumentModelList documentsByWebId = WebIdResolver.getDocumentsByWebId(coreSession, webid);
        if (CollectionUtils.isNotEmpty(documentsByWebId) && (documentsByWebId.size() == 1)) {
            doc = documentsByWebId.get(0);
        }
    
        if (log.isTraceEnabled()) {
            long end = System.currentTimeMillis();
            log.trace("      [getDocumentByWebId]: " + String.valueOf(end - begin) + " ms");
        }
        
        return doc.getPathAsString();
    }

    /**
     * Indique si l'exception donnée est engendrée par une exception de type
     * NoSuchDocumentException
     *
     * @return vrai si l'exception est a pour cause une NoSuchDocumentException
     */
/*    private boolean isNoSuchDocumentException(ClientException ce) {
        Throwable causeExc = ce.getCause();
        return causeExc instanceof NoSuchDocumentException;
    }*/

    private Blob createBlob(JSONArray json) {
        return new StringBlob(json.toString(), "application/json");
    }

    /**
     * Supprime le suffixe du nom d'un proxy.
     * 
     * @param path
     *            Chemin du proxy
     * @return le path avec le nom du proxy sans le suffixe
     */
    public static String computeNavPath(String path) {
        String result = path;
        if (path.endsWith(SUFFIXE_PROXY)) {
            result = result.substring(0, result.length() - SUFFIXE_PROXY.length());
        }
        return result;
    }

    /**
     * Indique si l'objet en entrée correspond à une erreur.
     * 
     * @param operationRes
     *            résultat d'une opération Nuxeo ou d'une méthode
     * @return vrai si l'objet en entrée correspond à une erreur
     */
    private static boolean isError(Object operationRes) {
        return (!(operationRes instanceof DocumentModel) && !(operationRes instanceof Boolean));
    }

    /**
     * Classe permettant de "tracer" une erreur serveur.
     */
    public static class ServeurException extends Exception {

        private static final long serialVersionUID = -2490817493963408580L;

        ServeurException() {
            super();
        }

        ServeurException(Exception e) {
            super(e);
        }

    }

    /**
	 * 
	 */
    private static class UnrestrictedFecthPubliInfosRunner extends UnrestrictedSessionRunner {

        private DocumentModel document;
        private Object liveDocRes;
        private JSONObject infosPubli;
        private List<Integer> errorsCodes;
        private UserManager userManager;

        /**
         * @return the infosPubli
         */
        public JSONObject getInfosPubli() {
            return infosPubli;
        }

        /**
         * @return the errorsCodes
         */
        public List<Integer> getErrorsCodes() {
            return errorsCodes;
        }

        public UnrestrictedFecthPubliInfosRunner(CoreSession session, DocumentModel document, Object liveDocRes, JSONObject infosPubli,
                UserManager userManager, List<Integer> errorsCodes) {
            super(session);
            this.document = document;
            this.liveDocRes = liveDocRes;
            this.infosPubli = infosPubli;
            this.errorsCodes = errorsCodes;
            this.userManager = userManager;
        }

        @Override
        public void run()  {
            /*try {*/
                if (!isError(liveDocRes)) {
                    DocumentModel liveDoc = (DocumentModel) this.liveDocRes;
                    /*
                     * Récupération du spaceID
                     */
                    /*this.infosPubli.put("spaceID", getSpaceID(liveDoc));*/

                    /*
                     * Récupération du parentSpaceID
                     */
                    /*String parentSpaceID = "";
                    DocumentModelList spaceParentList = ToutaticeDocumentHelper.getParentSpaceList(this.session, liveDoc, true, true);
                    if ((spaceParentList != null) && (spaceParentList.size() > 0)) {
                        DocumentModel parentSpace = spaceParentList.get(0);
                        parentSpaceID = getSpaceID(parentSpace);
                    }
                    this.infosPubli.put("parentSpaceID", parentSpaceID);*/
                }

                /*
                 * Récupération du contexte propre à l'appel d'autres opérations
                 * Nuxeo
                 */
                AutomationService automation = null;
                try {
                    automation = Framework.getService(AutomationService.class);
                } catch (Exception e) {
                    log.warn("Error getting automation service, error: " + e.getMessage());
                    //throw new ServeurException(e);
                }
                OperationContext ctx = new OperationContext(this.session);

                /* Appel à l'opération FetchPublishSpace */
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("value", this.document);

                Object fetchPublishSpaceRes = null;
                try {
                    fetchPublishSpaceRes = ToutaticeDocumentHelper.callOperation(automation, ctx, "Document.FetchPublishSpace", parameters);
                    DocumentModel publishSpaceDoc = (DocumentModel) fetchPublishSpaceRes;
                    this.infosPubli.element("publishSpaceType", publishSpaceDoc.getType());
                    this.infosPubli.element("publishSpacePath", URLEncoder.encode(computeNavPath(publishSpaceDoc.getPathAsString()), "UTF-8"));
                    /*try {*/
                        this.infosPubli.element("publishSpaceDisplayName", URLEncoder.encode(publishSpaceDoc.getTitle(), "UTF-8"));
                        BooleanProperty property = getInContextualizationProperty(publishSpaceDoc);
                        this.infosPubli.element("publishSpaceInContextualization", property.getValue());
                    /*} catch (ClientException e) {
                        this.infosPubli.element("publishSpaceInContextualization", Boolean.FALSE);
                        this.errorsCodes = manageException(errorsCodes, publishSpaceDoc, e, ERROR_PUBLISH_SPACE_FORBIDDEN,
                                "fetch publish space name or contextualization property for space ");
                    }*/
                } catch (Exception e) {
                    this.errorsCodes.add(getErrorCode(e, ERROR_PUBLISH_SPACE_NOT_FOUND, ERROR_PUBLISH_SPACE_FORBIDDEN));
                    this.infosPubli.element("publishSpaceInContextualization", Boolean.FALSE);
                    this.infosPubli.element("publishSpaceType", "");
                    this.infosPubli.element("publishSpacePath", "");
                    this.infosPubli.element("publishSpaceDisplayName", "");
                }

                /* Récupération du workspace contenant le document */
                parameters.clear();
                parameters.put("document", document);
                Object workspaceRes = null;
                try {
                    workspaceRes = ToutaticeDocumentHelper.callOperation(automation, ctx, "Document.FetchWorkspaceOfDocument", parameters);
                    DocumentModel workspace = (DocumentModel) workspaceRes;
                    this.infosPubli.element("workspacePath", URLEncoder.encode(workspace.getPathAsString(), "UTF-8"));
/*                    try {*/
                        this.infosPubli.element("workspaceDisplayName", URLEncoder.encode(workspace.getTitle(), "UTF-8"));
/*                    } catch (ClientException e) {
                        this.errorsCodes = manageException(errorsCodes, workspace, e, ERROR_WORKSPACE_FORBIDDEN,
                                "fetch workspace name or contextualization property for workspace");
                    }*/

                } catch (Exception e) {
                    /* Cas d'erreur */
                    this.infosPubli.element("workspaceInContextualization", Boolean.FALSE);
                    this.infosPubli.element("workspacePath", "");
                    this.infosPubli.element("workspaceDisplayName", "");
                    this.errorsCodes.add(getErrorCode(e, ERROR_WORKSPACE_NOT_FOUND, ERROR_WORKSPACE_FORBIDDEN));
                }

                /* TODO: valeur toujours mise à true pour l'instant */
                this.infosPubli.element("workspaceInContextualization", Boolean.TRUE);
                
                if (!isError(liveDocRes)) {
                    DocumentModel liveDoc = (DocumentModel) this.liveDocRes;
                    Boolean isRemotePublishable = isRemotePublishable(liveDoc, workspaceRes);
                    infosPubli.put("isRemotePublishable", isRemotePublishable);
					if(isRemotePublishable) {
				    	Boolean isRemotePublished = isRemotePublished(liveDoc);
				    	infosPubli.put("isRemotePublished", isRemotePublished);
				    }
				    else {
				    	infosPubli.put("isRemotePublished", Boolean.FALSE);
				    }
                }

                // Case of local publication
                // and "mono" remote publication
                // (cause in this case, this.document path is remote published path
                // and getProxy return it).
                DocumentModel publishedDoc = null;
                try {

                    publishedDoc = ToutaticeDocumentHelper.getProxy(session, document, SecurityConstants.READ);

                    Boolean isBeingModified = Boolean.FALSE;
                    if (publishedDoc != null) {
                        // Local proxy
                        isBeingModified = Boolean.valueOf(!publishedDoc.getVersionLabel().equals(infosPubli.get("liveVersion")));
                    } else {
                        // Local Publishing: live is in PublishSpace
                        String publishSpacePath = URLDecoder.decode(this.infosPubli.getString("publishSpacePath"), "UTF-8");
                        if (StringUtils.isNotEmpty(publishSpacePath)) {
                            isBeingModified = Boolean.TRUE;
                        }
                        //} else {
                            // Remote publishing
                        	// #1446 - Désactivation de la vérification sur les proxy distants, code non threadsafe
                        	
                            //isBeingModified = Boolean.valueOf(isLiveModifiedFromProxies(document));
                        //}
                    }

                    this.infosPubli.element("isLiveModifiedFromProxy", isBeingModified);
                    this.infosPubli.element("proxyVersion", publishedDoc != null ? publishedDoc.getVersionLabel() : "0.0");
                    this.infosPubli.element("published", publishedDoc != null ? Boolean.TRUE : Boolean.FALSE);
                } catch (Exception e) {
                    this.infosPubli.element("isLiveModifiedFromProxy", Boolean.TRUE);
                    this.infosPubli.element("published", Boolean.FALSE);
                }


                Object isAnonymousRes = isAnonymous(this.session, this.userManager, this.document, this.infosPubli);
                if (isError(isAnonymousRes)) {
                    this.infosPubli = (JSONObject) isAnonymousRes;
                } else {
                    this.infosPubli.element("anonymouslyReadable", isAnonymousRes);
                }

/*            } catch (Exception e) {
                throw new ClientException(e);
            }*/

        }
        
        /**
         * @param liveDoc given document
         * @return true if given document is remote publishable
         */
        private Boolean isRemotePublishable(DocumentModel liveDoc, Object workspaceRes) {
            boolean is = false;

            // live is in workspace
            if (workspaceRes instanceof DocumentModel) {
                is = Boolean.valueOf(!liveDoc.isFolder() && liveDoc.hasFacet("Publishable") && !liveDoc.isImmutable());
            }

            return is;
        }

		/**
		 * @param liveDoc given document
		 * @return true if given document is remote published
		 */
		private boolean isRemotePublished(DocumentModel liveDoc) {
			DocumentModelList remotePublishedDocuments = ToutaticeDocumentHelper.getRemotePublishedDocuments(this.session, liveDoc);
			if(remotePublishedDocuments.size() > 0) {
				return true;
			} else {
                return false;
			}
		}

        /**
         * Use in remote publication case.
         * 
         * @return true if live is different from all
         *         its published versions.
         */
        private Boolean isLiveModifiedFromProxies(DocumentModel liveDoc) {
            Boolean isModified = Boolean.TRUE;

            PublisherService publisherService = Framework.getService(PublisherService.class);
            Map<String, String> availablePublicationTrees = publisherService.getAvailablePublicationTrees();

            if (MapUtils.isNotEmpty(availablePublicationTrees)) {
                for (Entry<String, String> treeInfo : availablePublicationTrees.entrySet()) {
                    String treeName = treeInfo.getKey();

                    PublicationTree tree = publisherService.getPublicationTree(treeName, this.session, null);
                    List<PublishedDocument> publishedDocuments = tree.getExistingPublishedDocument(new DocumentLocationImpl(this.document));

                    for (PublishedDocument publishedDoc : publishedDocuments) {
                        DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDoc).getProxy();
                        if (liveDoc.getVersionLabel().equals(proxy.getVersionLabel())) {
                            isModified &= Boolean.FALSE;
                        }
                    }

                }
            }

            return isModified;
        }


        /**
         * Méthode permettant de vérifier si un document publié est accessible
         * de façon anonyme; met à faux le booléen anonymouslyReadable en cas
         * d'erreur.
         * 
         * @param session
         *            session Nuxeo
         * @param doc
         *            document dont on teste l'accès
         * @param infos
         *            pour stocker le résultat du test (booléen)
         * @return vrai si le document est accessible de façon anonyme
         * @throws ServeurException
         */
        private Object isAnonymous(CoreSession session, UserManager userManager, DocumentModel doc, JSONObject infos) {
            boolean isAnonymous = false;

/*            try {*/
                ACP acp = this.document.getACP();
                String anonymousId = userManager.getAnonymousUserId();
                Access access = acp.getAccess(anonymousId, SecurityConstants.READ);
                isAnonymous = access.toBoolean();
/*            } catch (ClientException e) {
                if (e instanceof DocumentSecurityException) {
                    infos.element("anonymouslyReadable", Boolean.FALSE);
                    return infos;
                } else {
                    log.warn("Failed to get ACP of document '" + doc.getPathAsString() + "', error:" + e.getMessage());
                    throw new ServeurException(e);
                }
            }*/

            return isAnonymous;
        }

        /**
         * Récupère la proriété de contextualisation (schéma toutatice) d'un
         * document.
         * 
         * @param doc
         *            document donné
         * @return la valeur de la propriété sous forme de BooleanProperty
         * @throws PropertyException
         */
        private BooleanProperty getInContextualizationProperty(DocumentModel doc) throws PropertyException {
            BooleanProperty property = (BooleanProperty) doc.getProperty(IN_CONTEXTUALIZATON_PROPERTY);
            return property;
        }

        /**
         * Gère le traitement d'une ClientException
         * 
         * @param errorsCodes
         *            pour stocker une erreur
         * @param doc
         *            pour générer un message dans les logs du serveur
         * @param ce
         *            exception à traiter
         * @param errorCode
         *            code d'erreur dans le cas d'une DocumentSecurityException
         *            (sous-classe de ClientException)
         * @param msg
         *            pour générer un message dans les logs du serveur
         * @throws ServeurException
         */
/*        private List<Integer> manageException(List<Integer> errorsCodes, DocumentModel doc, ClientException ce, int errorCode, String msg)
                throws ServeurException {
            if (ce instanceof DocumentSecurityException) {
                errorsCodes.add(errorCode);
            } else {
                log.warn("Failed" + msg + "'" + doc.getPathAsString() + "', error:" + ce.getMessage());
                throw new ServeurException(ce);
            }
            return errorsCodes;
        }*/

/*        // Règle de gestion de récupération du spaceID d'un document
        private String getSpaceID(DocumentModel document) {
            String spaceID = "";

            try {
                if (ToutaticeDocumentHelper.isASpaceDocument(document)) {
                    spaceID = document.getId();
                } else {
                    spaceID = safeString((String) document.getProperty("toutatice", "spaceID"));
                }
            } catch (ClientException e) {
                log.error("Failed to read the ttc:spaceID meta-data, error:" + e.getMessage());
            }

            return spaceID;
        }*/

    }

    private static String safeString(String value) {
        String safeValue = value;
        if (value == null) {
            safeValue = "";
        }
        return safeValue;
    }

}
