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
 *   mberhaut1
 *    
 */
package fr.toutatice.ecm.platform.automation;

import fr.toutatice.ecm.platform.core.constants.ToutaticeGlobalConst;
import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeCommentsHelper;
import fr.toutatice.ecm.platform.core.helper.ToutaticeSilentProcessRunnerHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;

import java.util.*;

@Operation(
        id = PublishDocument.ID,
        category = Constants.CAT_DOCUMENT,
        label = "Publish a document locally",
        description = "Publish the input document into the target section. Existing proxy is overrided if the override attribute is set. Return the created proxy.")
public class PublishDocument {

    public static final String ID = "Document.TTCPPublish";
    public static Log log = LogFactory.getLog(PublishDocument.class);

    @Context
    protected CoreSession session;

    @Param(name = "target", required = true)
    protected DocumentModel target;

    @Param(name = "override", required = false, values = "true")
    protected boolean override = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {

        InnerSilentPublish runner = new InnerSilentPublish(session, doc);
        runner.silentRun(true);
        return runner.getProxy();

    }

    private class InnerSilentPublish extends ToutaticeSilentProcessRunnerHelper {

        private DocumentModel doc;
        private DocumentModel newProxy;

        public DocumentModel getProxy() {
            return this.newProxy;
        }

        public InnerSilentPublish(CoreSession session, DocumentModel doc) {
            super(session);
            this.doc = doc;
        }

        @Override
        public void run() {

            this.newProxy = null;
            String formerProxyName = null;

            DocumentRef targetRef = target.getRef();
            DocumentRef baseDocRef = this.doc.getRef();

            GregorianCalendar lastIssuedDate = null;

            /** gestion du cycle de vie du document à publier */
            if (!this.doc.isVersion()) {
                // si le document est en projet: le valider
                if (ToutaticeNuxeoStudioConst.CST_DOC_STATE_PROJECT.equals(this.doc.getCurrentLifeCycleState())) {
                    this.doc.setPropertyValue("dc:valid", new Date());
                    this.session.saveDocument(this.doc);
                    this.session.followTransition(doc.getRef(), "approve");
                    this.doc.refresh(DocumentModel.REFRESH_STATE, null);
                }

                // si le document possède une version/archive 'en projet' (ex: s'il a été enregistré suite à une modification avec une montée de version): la
                // valider
                if (!this.doc.isCheckedOut()) {
                    String label = this.doc.getVersionLabel();
                    VersionModelImpl vm = new VersionModelImpl();
                    vm.setLabel(label);
                    DocumentModel vdoc = this.session.getDocumentWithVersion(this.doc.getRef(), vm);
                    if (null != vdoc && ToutaticeNuxeoStudioConst.CST_DOC_STATE_PROJECT.equals(vdoc.getCurrentLifeCycleState())) {
                        this.session.followTransition(vdoc.getRef(), "approve");
                    }
                }
            }

            if (null != targetRef) {
                /** conservation d'URL: récupérer le nom courant du proxy */
                if (this.doc.isVersion()) {
                    String sourceDocId = this.doc.getSourceId();
                    baseDocRef = new IdRef(sourceDocId);
                }

                DocumentModelList proxies = this.session.getProxies(baseDocRef, targetRef);
                Map<DocumentModel, List<DocumentModel>> proxyComments = new HashMap<DocumentModel, List<DocumentModel>>();

                if (proxies != null && proxies.size() >= 1) {
                    if (this.doc.isVersion() && override) {
                        DocumentModel proxy = proxies.get(0);
                        /* Récupération des commentaires */
                        proxyComments.putAll(ToutaticeCommentsHelper.getProxyComments(proxy));
                    }
                }
                /* Il ne doit y avoir qu'un seul proxy local */
                if (proxies != null && proxies.size() > 1) {
                    log.warn("Document " + this.doc.getPathAsString() + " has more than one local proxy. Deleting them.");
                }
                for (DocumentModel proxy : proxies) {
                    if (this.doc.isVersion() && override) {
                        this.session.removeDocument(proxy.getRef());
                    }

                    // Get last dc:issued date
                    GregorianCalendar issuedDate = (GregorianCalendar) proxy.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_XPATH_NUXEO_DC_ISSUED);
                    if (lastIssuedDate == null || issuedDate.after(lastIssuedDate)) {
                        lastIssuedDate = issuedDate;
                    }
                }

                /** publier */
                this.newProxy = this.session.publishDocument(doc, target, override);

                /* Mise à jour des commentaires */
                ToutaticeCommentsHelper.setComments(this.session, this.newProxy, proxyComments);
                /* We delete local live indicator */
                this.newProxy.removeFacet("isLocalPublishLive");
                
                this.session.saveDocument(this.newProxy);

                /** conservation d'URL: renommer le proxy (mise à jour de la propriété système "ecm:name") */
                if (!this.newProxy.getName().matches(".*\\" + ToutaticeGlobalConst.CST_PROXY_NAME_SUFFIX + "$")) {
                    String newProxyName = this.doc.getName() + ToutaticeGlobalConst.CST_PROXY_NAME_SUFFIX;
                    if (StringUtils.isNotBlank(formerProxyName)) {
                        newProxyName = formerProxyName;
                    }
                    this.newProxy = this.session.move(this.newProxy.getRef(), targetRef, newProxyName);
                }

                /** ordonner le document proxy */
                if (target.hasFacet("Orderable")) {
                    DocumentModel baseDoc = this.session.getDocument(baseDocRef);
                    this.session.orderBefore(targetRef, this.newProxy.getName(), baseDoc.getName());
                }


                /** positionner la date de publication */
                // #652 - Si une date de publication fonctionnelle existe, elle est appliquée.
                boolean yetPublished = lastIssuedDate != null;
                GregorianCalendar issued = null;
                if (this.doc.getPropertyValue("ttc:publicationDate") != null) {
                    // Check on working copy (form submit)
                    issued = (GregorianCalendar) doc.getPropertyValue("ttc:publicationDate");
                } else {
                    if (!yetPublished) {
                        // Never published
                        issued = new GregorianCalendar();
                    } else {
                        // New version is created: take precendent date
                        issued = lastIssuedDate;
                    }
                }
                
                if (issued != null) {
                    // Get pointed version
                    String srcDocId = this.newProxy.getSourceId();
                    DocumentModel srcDoc = this.session.getDocument(new IdRef(srcDocId));

                    srcDoc.setPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_XPATH_NUXEO_DC_ISSUED, issued);
                    srcDoc = this.session.saveDocument(srcDoc);
                }
                
            } else {
                throw new RuntimeException("Failed to get the target document reference");
            }
        }

    }

}
