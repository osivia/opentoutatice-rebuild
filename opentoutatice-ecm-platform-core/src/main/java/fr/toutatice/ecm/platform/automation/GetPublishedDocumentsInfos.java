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
 * dchevrier
 */
package fr.toutatice.ecm.platform.automation;

import java.util.Map;
import java.util.Map.Entry;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.core.publish.PublishedDocumentsFinder;



/**
 * @author David Chevrier.
 *
 */
@Operation(id = GetPublishedDocumentsInfos.ID, category = Constants.CAT_DOCUMENT, label = "GetPublishedDocumentsInfos",
        description = "Get informations of remote published documents of a given live document.")
public class GetPublishedDocumentsInfos {

    /** Logger. */
    private static final Log log = LogFactory.getLog(GetPublishedDocumentsInfos.class);

    /** Identifier. */
    public static final String ID = "Document.GetPublishedDocumentsInfos";

    @Context
    protected CoreSession session;

    @Param(name = "readFilter", required = false)
    protected Boolean readFilter;

    @OperationMethod
    public StringBlob run(DocumentModel document) throws Exception {
        // For Trace logs
        long begin = System.currentTimeMillis();

        JSONArray informations = new JSONArray();
        GetUnrestrictedSections getter = new GetUnrestrictedSections(this.session, PublishedDocumentsFinder.getInstance(), document);

        if (BooleanUtils.isTrue(readFilter)) {
            getter.run();
        } else {
            getter.runUnrestricted();
        }

        informations = getter.getInformations();

        if (log.isTraceEnabled()) {
            long end = System.currentTimeMillis();
            log.trace("      [GetPublishedDocumentsInfos]: " + String.valueOf(end - begin) + " ms");
        }

        return new StringBlob(informations.toString(), "application/json");
    }

    public static final String WEBID_PATTERN = "webidpattern";
    public static final String WEB_APP = "/nuxeo/";

    private static class GetUnrestrictedSections extends UnrestrictedSessionRunner {

        /** Remote published documents finder. */
        private PublishedDocumentsFinder finder;
        private DocumentModel document;
        private JSONArray informations;

        protected GetUnrestrictedSections(CoreSession session, PublishedDocumentsFinder finder, DocumentModel document) {
            super(session);
            this.finder = finder;
            this.document = document;
            this.informations = new JSONArray();
        }

        public JSONArray getInformations() {
            return this.informations;
        }

        @Override
        public void run() {

            this.informations = getSectionsInfos(this.finder, this.session, this.document, this.informations);

        }

        /**
         * Gets Sections infos.
         */
        protected JSONArray getSectionsInfos(PublishedDocumentsFinder finder, CoreSession session, DocumentModel document, JSONArray informations) {
            // Logs
            final long b = System.currentTimeMillis();

            // Get published documents (remote proxies) with their section
            Map<DocumentRef, PublishedDocument> publishedDocumentsIn = finder.find(session, document);

            if (log.isDebugEnabled()) {
                final long e = System.currentTimeMillis();
                log.debug("#getSectionsInfos | publishedDocumentsIn: " + String.valueOf(e - b) + "ms");
            }

            // Logs
            final long b1 = System.currentTimeMillis();

            // Build infos
            JSONObject documentInfos = new JSONObject();
            for (Entry<DocumentRef, PublishedDocument> entryRef : publishedDocumentsIn.entrySet()) {
                // Section's title
                DocumentModel section = ToutaticeDocumentHelper.getUnrestrictedDocument(session, entryRef.getKey().toString());
                documentInfos.element("sectionTitle", section.getTitle());

                // Published document infos
                DocumentModel proxy = ((SimpleCorePublishedDocument) entryRef.getValue()).getProxy();
                documentInfos.element("url", getDocumentURL(proxy));
                documentInfos.element("versionLabel", proxy.getVersionLabel());

                informations.add(documentInfos);

            }

            if (log.isDebugEnabled()) {
                final long e1 = System.currentTimeMillis();
                log.debug("#getSectionsInfos | buildJson: " + String.valueOf(e1 - b1) + "ms");
            }


            return informations;
        }

    }

    /**
     * @param document
     * @return Nuxeo URL of document with webid.
     *         If not, URL is enmpty.
     */
    protected static String getDocumentURL(DocumentModel document) {
        String url = StringUtils.EMPTY;

        if (document != null) {
            url = DocumentModelFunctions.documentUrl(document);

            if (StringUtils.isNotBlank(url)) {
                url = WEB_APP + url;
            }
        }

        return url;
    }

}
