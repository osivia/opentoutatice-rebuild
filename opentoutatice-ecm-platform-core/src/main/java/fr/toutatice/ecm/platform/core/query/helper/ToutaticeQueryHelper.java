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
 *   lbillon
 *   dchevrier
 *    
 */
package fr.toutatice.ecm.platform.core.query.helper;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;


/**
 * @author david chevrier
 *
 */
public class ToutaticeQueryHelper {
    
    /*
     * Helper class.
     */
    private ToutaticeQueryHelper(){};
    
    /**
     * Execute a query in unrestricted mode.
     * @param query
     * @return DocumentModelList
     */
    public static DocumentModelList queryUnrestricted(CoreSession session, String query){
        UnrestrictedQueryRunner runner = new UnrestrictedQueryRunner(session, query);
        return runner.runQuery();
    }
    
    /**
     * Execute a query in unrestricted mode.
     * 
     * @param query
     * @param limit
     * @return DocumentModelList
     */
    public static DocumentModelList queryUnrestricted(CoreSession session, String query, int limit) {
        UnrestrictedQueryRunner runner = new UnrestrictedQueryRunner(session, query, limit);
        return runner.runQuery();
    }

    /**
     * To query in unrestricted mode.
     * 
     * @author David Chevrier.
     *
     */
    public static class UnrestrictedQueryRunner extends UnrestrictedSessionRunner {

        String query;
        int limit = -1;

        DocumentModelList docs;

        protected UnrestrictedQueryRunner(CoreSession session, String query) {
            super(session);
            this.query = query;
        }

        protected UnrestrictedQueryRunner(CoreSession session, String query, int limit) {
            super(session);
            this.query = query;
            this.limit = limit;
        }

        @Override
        public void run()  {
            this.docs = this.session.query(this.query, this.limit);

            for (DocumentModel documentModel : this.docs) {
                documentModel.detach(true);
            }
        }

        public DocumentModelList runQuery() {
            runUnrestricted();
            return this.docs;
        }
    }

}
