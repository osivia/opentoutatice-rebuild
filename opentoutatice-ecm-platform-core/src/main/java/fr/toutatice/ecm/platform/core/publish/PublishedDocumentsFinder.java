/**
 * 
 */
package fr.toutatice.ecm.platform.core.publish;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.query.helper.ToutaticeEsQueryHelper;


/**
 * @author david
 *
 */
public class PublishedDocumentsFinder {

    /** Logger. */
    private static final org.apache.commons.logging.Log log = LogFactory.getLog(PublishedDocumentsFinder.class);

    /** Query to get published documents. */
    // FIXME: replace ecm:name by ecm:proxyVersionableId so demormalized it
    private static final String PUBLISHED_DOCS_QUERY = "select * from Document where ecm:isProxy = 1 and ttc:webid = '%s' and ecm:mixinType <> 'HiddenInNavigation'";

    /** Singleton. */
    private static PublishedDocumentsFinder instance;

    /** Published documents of working copy. */
    private Map<DocumentRef, PublishedDocument> publishedDocumentsIn;

    /**
     * Constructor.
     */
    private PublishedDocumentsFinder() {
        super();
    }

    public static synchronized PublishedDocumentsFinder getInstance() {
        if (instance == null) {
            instance = new PublishedDocumentsFinder();
        }
        return instance;
    }

    /**
     * Gets published document of current working copy.
     * 
     * @return
     */
    public Map<DocumentRef, PublishedDocument> find(CoreSession userSession, DocumentModel workingDoc) {
        // Logs
        final long b = System.currentTimeMillis();

        String query = String.format(PUBLISHED_DOCS_QUERY, (String) workingDoc.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID));
        DocumentModelList proxies = ToutaticeEsQueryHelper.query(userSession, query);

        if (log.isDebugEnabled()) {
            final long e = System.currentTimeMillis();
            log.debug("#find | ES: " + String.valueOf(e - b) + " ms");
        }

        // ==================
        // Logs
        // final long bx = System.currentTimeMillis();
        //
        // proxies = ToutaticeEsQueryHelper.query(userSession, query, -1, false, true);
        //
        // if (log.isDebugEnabled()) {
        // final long ex = System.currentTimeMillis();
        // log.debug("#find | ES & fetch from ES: " + String.valueOf(ex - bx) + " ms");
        // }
        //
        // // =================
        //
        // final long b1 = System.currentTimeMillis();
        //
        // proxies = ToutaticeQueryHelper.queryUnrestricted(userSession, query);
        //
        // if (log.isDebugEnabled()) {
        // final long e1 = System.currentTimeMillis();
        // log.debug("#find | VCS: " + String.valueOf(e1 - b1) + " ms");
        // }

        // Adapt
        this.publishedDocumentsIn = new HashMap<>(proxies.size());
        for (DocumentModel proxy : proxies) {
            this.publishedDocumentsIn.put(proxy.getParentRef(), new SimpleCorePublishedDocument(proxy));
        }

        return this.publishedDocumentsIn;
    }

    /**
     * @return the publishedDocuments
     */
    public synchronized Map<DocumentRef, PublishedDocument> getPublishedDocumentsIn() {
        return publishedDocumentsIn;
    }

    /**
     * 
     * @return
     */
    public synchronized PublishedDocument getPublishedDocumentIn(DocumentRef parentRef) {
        return publishedDocumentsIn.get(parentRef);
    }

    /**
     * Refreshes list of published documents.
     */
    public synchronized void refresh() {
        if (this.publishedDocumentsIn != null) {
            this.publishedDocumentsIn.clear();
        }
    }


}
