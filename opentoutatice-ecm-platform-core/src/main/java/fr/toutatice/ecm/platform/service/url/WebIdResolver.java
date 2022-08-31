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
package fr.toutatice.ecm.platform.service.url;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.query.helper.ToutaticeEsQueryHelper;


/**
 * Manage the resolution of a document given its webId.
 * 
 * @author david chevrier
 *
 */
public class WebIdResolver {

    /** Logger. */
    private static final Log log = LogFactory.getLog(WebIdResolver.class);

    /** Remote proxy webid marker. */
    public static final String RPXY_WID_MARKER = "_c_";

    /** Utility class. */
    private WebIdResolver() {
    };

    /**
     * @param coreSession
     * @param webId
     * @return unique live document given its webid.
     * @throws NoSuchDocumentException 
     */
    public static DocumentModel getLiveDocumentByWebId(CoreSession coreSession, String webId) throws NoSuchDocumentException {
        DocumentModel live = null;
        
        if(StringUtils.isNotBlank(webId)) {
        
	        if(log.isDebugEnabled()) {
	        	log.debug("Resolving live document by webid: [".concat(webId).concat("]..."));
	        }
	
	        String query = String.format(ToutaticeWebIdHelper.LIVE_WEB_ID_QUERY, webId);
	        DocumentModelList lives = ToutaticeEsQueryHelper.unrestrictedQuery(coreSession, query, 1);
	        
	        if (CollectionUtils.isNotEmpty(lives) && lives.size() == 1) {
	            live = lives.get(0);
	        } else  {
	        	throw new NoSuchDocumentException(webId);
	        }
	        
	        if(log.isDebugEnabled()) {
	        	if(live != null) {
	        		StringBuffer liveStr = new StringBuffer();
	        		liveStr.append(live.getType()).append(" | ")
	        			.append(live.getTitle() != null ? live.getTitle() : StringUtils.EMPTY).append(" | ")
	        			.append(live.getPathAsString());
	        		log.debug("Found live document with webId [".concat(webId).concat("]: ").concat("[").concat(liveStr.toString()).concat("]"));
	        	} else {
	        		log.debug("No live document found with webId: [".concat(webId).concat("]"));
	        	}
	        }
        }

        return live;
    }

    /**
     * @param coreSession
     * @param webId
     * @return document as remote proxy or live matching given webid (live or proxy, proxies).
     * @throws NoSuchDocumentException
     */
    public static DocumentModelList getDocumentsByWebId(CoreSession coreSession, String webId) throws NoSuchDocumentException {

        DocumentModelList documents = null;

        if (StringUtils.isNotBlank(webId)) {
        	
        	if(log.isDebugEnabled()) {
	        	log.debug("Resolving document by webid: [".concat(webId).concat("]..."));
	        }

            UnrestrictedFecthWebIdRunner fecthWebIdRunner = new UnrestrictedFecthWebIdRunner(coreSession, webId);
            fecthWebIdRunner.runUnrestricted();
            documents = fecthWebIdRunner.getDocuments();

            if (CollectionUtils.isEmpty(documents) || (CollectionUtils.isNotEmpty(documents) && documents.size() > 1)) {
                throw new NoSuchDocumentException(webId);
            }
            
            if(log.isDebugEnabled()) {
            	DocumentModel doc_ = documents.get(0);
	        	if(doc_ != null) {
	        		StringBuffer liveStr = new StringBuffer();
	        		liveStr.append(doc_.getType()).append(" | ")
	        			.append(doc_.getTitle() != null ? doc_.getTitle() : StringUtils.EMPTY).append(" | ")
	        			.append(doc_.getPathAsString());
	        		log.debug("Found live document with webId [".concat(webId).concat("]: ").concat("[").concat(liveStr.toString()).concat("]"));
	        	} else {
	        		log.debug("No live document found with webId: [".concat(webId).concat("]"));
	        	}
	        }

        }

        return documents;

    }

    /**
     * Get doc as remote proxy or live by webid in unrestricted mode (admin)
     */
    private static class UnrestrictedFecthWebIdRunner extends UnrestrictedSessionRunner {

        String webId;
        DocumentModelList documents;

        public UnrestrictedFecthWebIdRunner(CoreSession session, String webId) {
            super(session);
            this.webId = webId;
            this.documents = new DocumentModelListImpl();
        }

        @Override
        public void run() {
            if (StringUtils.contains(this.webId, RPXY_WID_MARKER)) {
                getRemoteProxy();
            } else {
                getLive();
            }
        }

        /**
         * Get remote proxy with given logical webid like
         * <webid_of_live>_c_<webid_of_section_of_remote_proxy>.
         */
        private void getRemoteProxy() {
            String[] webIds = StringUtils.splitByWholeSeparator(this.webId, RPXY_WID_MARKER);

            // Remote proxy webid is same as live
            String liveWId = webIds[0];
            // Webid of section where live is published (section is parent of remote proxy)
            String sectionWId = webIds[1];

            // Get proxy(ies) with live webId
            DocumentModelList rProxies = ToutaticeEsQueryHelper.unrestrictedQuery(this.session, String.format(ToutaticeWebIdHelper.RPXY_WEB_ID_QUERY, liveWId), 1);

            // Published in one place only only
            if (rProxies.size() == 1) {
                // Proxy found
                DocumentModel rPxy = rProxies.get(0);
                // Check parent
                if (isParentWebId(rPxy, sectionWId)) {
                    this.documents.add(rPxy);
                }
            } else if (rProxies.size() > 1) {
                // Published in many places.
                // Check all to see incoherences (this.documents.size() must be equals to one)
                for (DocumentModel rPxy : rProxies) {
                    if (isParentWebId(rPxy, sectionWId)) {
                        this.documents.add(rPxy);
                    }
                }
            }
        }

        /**
         * Get live with given webId.
         */
        private void getLive() {
            DocumentModelList lives = ToutaticeEsQueryHelper.unrestrictedQuery(this.session, String.format(ToutaticeWebIdHelper.LIVE_WEB_ID_QUERY, this.webId), 1);
            if (CollectionUtils.isNotEmpty(lives) && lives.size() == 1) {
                this.documents.add(lives.get(0));
            }
        }

        /**
         * Checks if webid is parent's document one.
         * 
         * @param document
         * @param webId
         * @return true if webid is parent's document one
         */
        public boolean isParentWebId(DocumentModel document, String webId) {
            boolean is = false;
            if (document != null) {
                DocumentModel parentDocument = this.session.getParentDocument(document.getRef());
                if (parentDocument != null) {
                    String pWebId = (String) parentDocument.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
                    is = StringUtils.equals(webId, pWebId);
                }
            }
            return is;
        }


        public DocumentModelList getDocuments() {
            return this.documents;
        }

    }


}
