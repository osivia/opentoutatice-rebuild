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
 * lbillon
 */
package fr.toutatice.ecm.platform.automation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
//import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.event.EventService;
//import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.core.uidgen.UIDGenerator;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeSilentProcessRunnerHelper;
import fr.toutatice.ecm.platform.service.url.ToutaticeWebIdHelper;
import fr.toutatice.ecm.platform.service.webid.TTCUIDGeneratorService;

/**
 * Generate or apply a webId on a document. Check if the webId is unique in the domain
 * 
 * @author loic
 * 
 */
@Operation(id = SetWebID.ID, category = Constants.CAT_DOCUMENT, label = "Set webid.",
        description = "Check unicity of webid and apply to the document in current domain..")
public class SetWebID {

    /** Op ID */
    public static final String ID = "Document.SetWebId";

    private static final Log log = LogFactory.getLog(SetWebID.class);
//
//    @Deprecated
//    private static final String NO_RECURSIVE_CHAIN = "notRecursive";

    private static final List<Class<?>> FILTERED_SERVICES_LIST = new ArrayList<Class<?>>() {

        private static final long serialVersionUID = 1L;

        {
            add(EventService.class);
            add(VersioningService.class);
        }
    };

    @Context
    protected CoreSession coreSession;

//    @In(create = true)
//    protected NavigationContext navigationContext;

//    @Deprecated
//    @Param(name = "chainSource", required = false)
//    protected String chainSource;

    /**
     * Main method
     * 
     * @return document modified
     * @throws Exception
     */
    @OperationMethod()
    public DocumentModel run(DocumentModel document) throws Exception {
        //UnrestrictedSilentSetWebIdRunner runner = new UnrestrictedSilentSetWebIdRunner(this.coreSession, document, this.chainSource);
    	UnrestrictedSilentSetWebIdRunner runner = new UnrestrictedSilentSetWebIdRunner(this.coreSession, document);
    	runner.silentRun(true, FILTERED_SERVICES_LIST);
        return runner.getDocument();
    }

    public static class UnrestrictedSilentSetWebIdRunner extends ToutaticeSilentProcessRunnerHelper {

        private DocumentModel document;
//        @Deprecated
//        private String chainSource;
        private DocumentModel parentDoc;

        public UnrestrictedSilentSetWebIdRunner(CoreSession session, DocumentModel document) {
            super(session);
            this.document = document;
        }

//        @Deprecated
//        public UnrestrictedSilentSetWebIdRunner(CoreSession session, DocumentModel document, String chainSource) {
//            super(session);
//            this.document = document;
//            this.chainSource = chainSource;
//        }

        public DocumentModel getDocument() {
            return this.parentDoc != null ? this.parentDoc : this.document;
        }

        @Override
        public void run() {
            // Performances logs
            final long begin = System.currentTimeMillis();

            String webId = null;
            String extension = null;
            boolean creationMode = false;
            boolean hasToBeUpdated = false;

            // if document has not toutatice schema
            if (this.document.isImmutable() || !this.document.hasSchema(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE)) {
                return;
            }

//            try {
                webId = getWebId();

                // in creation or import, try to generate it.
                if (StringUtils.isBlank(webId)) {
                    // else new id is generated
                    webId = generateWebId();
                    extension = getBlobExtensionIfExists(webId);
                    creationMode = true;
                }

                // TODO: move in generator from dc:title
                // // webid setted in the document, we use it
                // else if (StringUtils.isNotBlank(getWebId())) {
                //
                // webId = getWebId().toString();
                // // clean if needed
                // // DCH: TEST procedures!
                // if (StringUtils.contains(webId, "_")) {
                // // Case of technical webId
                // String nonTechPart = StringUtils.substringAfterLast(webId, "_");
                // String techPart = StringUtils.substringBeforeLast(webId, "_");
                // webId = techPart.concat("_").concat(IdUtils.generateId(nonTechPart, "-", true, 512));
                // } else {
                // webId = IdUtils.generateId(webId, "-", true, 512);
                // }
                //
                // }

                while (isNotUnique(this.session, this.document, webId)) {
                    webId = generateWebId();
                    hasToBeUpdated = true;
                }

//            } catch (DocumentException e) {
//                throw new ClientException(e);
//            }

            if (StringUtils.isNotBlank(webId)) {
                // [others ops like move, restore, ...] don't throw an exception, put a suffix after the id

                // save weburl
                if (hasToBeUpdated || (!hasToBeUpdated && creationMode)) {
                    log.info("Id relocated to " + webId + " for document " + this.document.getPathAsString());
                    this.document.setPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID, webId);
                    if (extension != null) {
                        this.document.setPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_XPATH_TOUTATICE_EXTENSION_URL, extension);
                    }
                    this.session.saveDocument(this.document);
                }
            }

            if (log.isDebugEnabled()) {
                final long end = System.currentTimeMillis();
                log.debug("[#run] " + String.valueOf(end - begin) + " ms");
            }

        }

        /**
         * @return
         */
        protected String getWebId() {
            return (String) this.document.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
        }

        /**
         * @return new random webId.
         */
        protected String generateWebId() {
            TTCUIDGeneratorService service = (TTCUIDGeneratorService) Framework.getRuntime().getComponent(TTCUIDGeneratorService.ID);
            UIDGenerator defaultGenerator = service.getDefaultUIDGenerator();
            return defaultGenerator.createUID(this.document);
        }

        /**
         * for Files or Pictures : put the extension of the file if exists.
         * 
         * @param webid
         * @return webid with extension.
         */
        protected String getBlobExtensionIfExists(String webid) {
            String extension = null;

            if ("File".equals(this.document.getType()) || "Picture".equals(this.document.getType())) {
                int lastIndexOf = this.document.getTitle().lastIndexOf(".");
                if (lastIndexOf > -1) {
                    extension = this.document.getTitle().substring(lastIndexOf + 1, this.document.getTitle().length());
                }
            }
            return extension;
        }

        /**
         * Remove blob extension from webid (if exists).
         * 
         * @param webid
         * @param extension
         * @return wenid without blob extension.
         */
        protected String removeBlobExtensionIfExists(String webid, String extension) {
            if (StringUtils.isNotBlank(extension)) {
                if (webid.endsWith(extension)) {
                    webid = webid.substring(0, webid.length() - extension.length() - 1);
                }
            }
            return webid;
        }

        /**
         * Checks repository unicity of given webId.
         * 
         * @return true if not unique
         */
        public static boolean isNotUnique(CoreSession session, DocumentModel document, String webId) {
            // For logs
            final long begin = System.currentTimeMillis();

            String escapedWebId = StringEscapeUtils.escapeJava(webId);
            // Local proxy and live have same webId: check only live
            DocumentModelList query = session.query(String.format(ToutaticeWebIdHelper.LIVE_WEB_ID_UNICITY_QUERY, escapedWebId, document.getId()));

            if (log.isDebugEnabled()) {
                final long end = System.currentTimeMillis();
                log.debug("[#isNotUnique] " + String.valueOf(end - begin) + " ms");
            }

            return query.size() > 0;
        }

    }

}
