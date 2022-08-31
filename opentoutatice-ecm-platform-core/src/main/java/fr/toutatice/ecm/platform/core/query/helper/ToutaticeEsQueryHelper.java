/**
 * 
 */
package fr.toutatice.ecm.platform.core.query.helper;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;


/**
 * @author david
 *
 */
public class ToutaticeEsQueryHelper {
	
	private static final Log log = LogFactory.getLog(ToutaticeEsQueryHelper.class);

    /** Results limit. */
    public static final int DEFAULT_MAX_RESULT_SIZE = 10000;

    /** Descendants query. */
    public static final String DESCENDANTS_ID_QUERY = "select * from Document where ecm:path startswith '%s' %s and ecm:isVersion = 0 and ecm:mixinType <> 'HiddenInNavigation' and ecm:currentLifeCycleState <> 'deleted'";
    /** Proxies query clause. */
    public static final String PROXIES_CLAUSE = " and ecm:isProxy = 1 ";
    /** Lives query clause. */
    public static final String LIVES_CLAUSE = " and ecm:isProxy = 0 ";
    /** Not versions query clause. */
    public static final String NOT_VERSIONS_CLAUSE = " and ecm:isVersion = 0 ";


    /** ElasticSeach service for querying. */
    private static ElasticSearchService ess;

    /**
     * Getter for ElasticSearchService.
     */
    protected static ElasticSearchService getElasticSearchService() {
        if (ess == null) {
            ess = Framework.getService(ElasticSearchService.class);
        }
        return ess;
    }

    /**
     * Utility class.
     */
    private ToutaticeEsQueryHelper() {
        super();
    }

    /**
     * Gets all children (i.e. children an d recurse children of children) of document.
     * 
     * @param parent
     * @param unrestricted
     * @param published
     * @return all children of document
     */
    public static DocumentModelList getDescendants(CoreSession session, DocumentModel parent, boolean unrestricted, boolean published) {
        String clause = published ? PROXIES_CLAUSE : LIVES_CLAUSE;
        String nxql = String.format(DESCENDANTS_ID_QUERY, parent.getPathAsString(), clause);

        return query(session, nxql, -1, unrestricted, false);
    }
    
    public static IterableQueryResult unrestrictedQueryAndAggregate(CoreSession session, String nxql) {
        return unrestrictedQueryAndAggregate(session, nxql, false);
    }

    /**
     * Executes queryAndAggregate in unrestricted way.
     * 
     * @param session
     * @param nxql
     * @return IterableQueryResult
     */
    public static IterableQueryResult unrestrictedQueryAndAggregate(CoreSession session, String nxql, boolean fetchFromEs) {
        UnrestrictedQueryAndAggregate uQnA = new UnrestrictedQueryAndAggregate(session, nxql, fetchFromEs);
        uQnA.runUnrestricted();
        return uQnA.getRowsResults();
    }

    /**
     * Execute query on Es (with really no limit: {@link org.nuxeo.elasticsearch.query.NxQueryBuilder#limit(int) NxQueryBuilder.limit(int)}).
     * 
     * @param session
     * @param nxql
     * @return DocumentModelList fetched from DB
     */
    public static DocumentModelList query(CoreSession session, String nxql) {
        // Fetch docs from DB to get facets not defined in DocumentTypeImpl (they ares tored in hierachy table like isRemoteProxy)
        return query(session, nxql, 0, -1, false);
    }

    /**
     * Execute query on Es.
     * 
     * @param session
     * @param nxql
     * @param limit
     * @return DocumentModelList fetched from DB
     */
    public static DocumentModelList query(CoreSession session, String nxql, int limit) {
        // Fetch docs from DB to get facets not defined in DocumentTypeImpl (they ares tored in hierachy table like isRemoteProxy)
        return query(session, nxql, 0, limit, false);
    }

    /**
     * Execute paginated query on ES.
     * 
     * @param session
     * @param nxql
     * @param currentPageIndex
     * @param pageSize
     * @return DocumentModelList fetched from DB
     */
    public static DocumentModelList query(CoreSession session, String nxql, int currentPageIndex, int pageSize) {
        // Fetch docs from DB to get facets not defined in DocumentTypeImpl (they ares tored in hierachy table like isRemoteProxy)
        return query(session, nxql, currentPageIndex, pageSize, false);
    }

    /**
     * Execute paginated query on ES.
     * 
     * @param session
     * @param nxql
     * @param currentPageIndex
     * @param pageSize
     * @return DocumentModelList fetched from Es or from DB
     */
    public static DocumentModelList query(CoreSession session, String nxql, int currentPageIndex, int pageSize, boolean fetchDocFromEs) {
        // Builder
        NxQueryBuilder qB = new NxQueryBuilder(session).nxql(nxql);

        // Fetch documents from Es
        if (fetchDocFromEs) {
            qB.fetchFromElasticsearch();
        } else {
            qB.fetchFromDatabase();
        }
        // Pagination
        if (pageSize > 0 && currentPageIndex >= 0) {
            qB.offset(currentPageIndex * pageSize);
            qB.limit(pageSize);
        } else {
            qB.limit(DEFAULT_MAX_RESULT_SIZE);
        }

        // Query
        return getElasticSearchService().query(qB);
    }

    public static DocumentModelList unrestrictedQuery(CoreSession session, String nxql, int limit) {
        UnrestrictedQuery uQry = new UnrestrictedQuery(session);
        uQry.setNxql(nxql);
        uQry.setPageSize(limit);

        uQry.runUnrestricted();

        return uQry.getDocuments();
    }

    public static DocumentModelList query(CoreSession session, String nxql, int limit, boolean unrestricted, boolean fetchDocFromEs) {
        // Init
        UnrestrictedQuery uQry = new UnrestrictedQuery(session);
        uQry.setNxql(nxql);
        uQry.setPageSize(limit);
        uQry.setFetchDocFromEs(fetchDocFromEs);

        // Run
        if (unrestricted) {
            uQry.runUnrestricted();
        } else {
            uQry.run();
        }

        return uQry.getDocuments();
    }


    public static class UnrestrictedQueryAndAggregate extends UnrestrictedSessionRunner {
        /** Query. */
        private String query;

        /** Limit of results. */
        private int limit = -1;
        /** Results rows. */
        private IterableQueryResult iqr;
        /** Fetch from ES indicator. */
        private boolean fetchFromEs = false;

        /** Constructor. */
        protected UnrestrictedQueryAndAggregate(CoreSession session) {
            super(session);
        }

        /** Constructor. */
        protected UnrestrictedQueryAndAggregate(CoreSession session, String query, boolean fetchFromEs) {
            super(session);
            this.query = query;
            this.fetchFromEs = fetchFromEs;
        }

        /** Setter for nxql query. */
        public void setQuery(String query) {
            this.query = query;
        }

        /** Setter for limit of query's results. */
        public void setLimit(int limit) {
            this.limit = limit;
        }

        /** Getter fir iterable query results. */
        public IterableQueryResult getRowsResults() {
            return this.iqr;
        }

        @Override
        public void run()  {
            if (StringUtils.isNotBlank(this.query)) {
                // ES query
                ElasticSearchService ess = ToutaticeEsQueryHelper.getElasticSearchService();

                NxQueryBuilder queryBuilder = new NxQueryBuilder(super.session).nxql(this.query).limit(this.limit);
                if(this.fetchFromEs) {
                	queryBuilder.fetchFromElasticsearch();
                }
                
                this.iqr = ess.queryAndAggregate(queryBuilder).getRows();
            } else {
                log.error("No query defined.");
                //throw new Exception("No query defined.");
            }
        }

    }

    public static class UnrestrictedQuery extends UnrestrictedSessionRunner {
        /** Query. */
        private String nxql;

        /** Current page index. */
        private int currentPageIndex = 0;

        /** Page size. */
        private int pageSize = -1;

        /** Fetch documents from ES (i.e only simple properties). */
        private boolean fetchDocFromEs = false;

        /** Results. */
        private DocumentModelList documents;

        protected UnrestrictedQuery(CoreSession session) {
            super(session);
        }


        /**
         * @param nxql the nxql to set
         */
        public void setNxql(String nxql) {
            this.nxql = nxql;
        }


        /**
         * @param currentPageIndex the currentPageIndex to set
         */
        public void setCurrentPageIndex(int currentPageIndex) {
            this.currentPageIndex = currentPageIndex;
        }


        /**
         * @param pageSize the pageSize to set
         */
        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }


        /**
         * @return the fetchDocFromEs
         */
        public boolean isFetchDocFromEs() {
            return fetchDocFromEs;
        }


        /**
         * @param fetchDocFromEs the fetchDocFromEs to set
         */
        public void setFetchDocFromEs(boolean fetchDocFromEs) {
            this.fetchDocFromEs = fetchDocFromEs;
        }

        /**
         * @return the documents
         */
        public DocumentModelList getDocuments() {
            return documents;
        }

        @Override
        public void run() {
        	// For monitoring
        	long b_ = System.currentTimeMillis();
        	
            this.documents = ToutaticeEsQueryHelper.query(super.session, this.nxql, this.currentPageIndex, this.pageSize, this.fetchDocFromEs);
            
            if(log.isDebugEnabled()) {
            	long e_ = System.currentTimeMillis();
            	String query_ = this.nxql != null ? this.nxql : "null";
            	log.debug("Query [".concat(query_).concat("] executed on ES in [".concat(String.valueOf(e_ - b_).concat(" ms]"))) );
            }
        }

    }

}
