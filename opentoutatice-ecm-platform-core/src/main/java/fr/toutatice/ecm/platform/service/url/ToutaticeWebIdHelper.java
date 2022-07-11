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
 * dchevrier
 * lbillon
 * 
 */
package fr.toutatice.ecm.platform.service.url;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;


/**
 * @author David Chevrier.
 *
 */
public class ToutaticeWebIdHelper {
    
    /** Live query filter. */
    public static final String LIVE_QUERY_FILTER = " and ecm:isProxy = 0 ";
    /** Remote proxy query filter. */
    public static final String RPXY_QUERY_FILTER = " and (ecm:isProxy = 1 and ecm:mixinType = 'isRemoteProxy') ";
    /** Unicity webid query filter. */
    public static final String WEB_ID_UNICITY_QUERY_FILTER = " AND ecm:uuid <> '%s' ";
    
    /** Query to get document according to its webId. */
    public static final String WEB_ID_QUERY = "select * from Document where ttc:webid = '%s' "
            + "AND ecm:isVersion = 0";
    /** Query to get live document according to its webId. */
    public static final String LIVE_WEB_ID_QUERY = WEB_ID_QUERY + LIVE_QUERY_FILTER;
    /** Query to get remote proxiex with given webid. */
    public static final String RPXY_WEB_ID_QUERY = WEB_ID_QUERY + RPXY_QUERY_FILTER;
    
    /** Unicity webid query. */
    public static final String WEB_ID_UNICITY_QUERY = WEB_ID_QUERY + WEB_ID_UNICITY_QUERY_FILTER;
    /** Unicity webid query. */
    public static final String LIVE_WEB_ID_UNICITY_QUERY = WEB_ID_UNICITY_QUERY + LIVE_QUERY_FILTER;
    
    /**
     * Utility class.
     */
    private ToutaticeWebIdHelper(){};
    
    /**
     * 
     * @param session
     * @param document
     * @return the first parent of document with webId.
     */
    @Deprecated
    public static String getParentWebId(CoreSession session, DocumentModel document){
        String parentWebId = StringUtils.EMPTY;
        
        Filter webIdExists = new Filter(){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean accept(DocumentModel docModel) {
                String parentWebId = (String) docModel.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
                return StringUtils.isNotBlank(parentWebId);
            }
            
        };
        
        // Carefull: list is fetched in unrestricted way.
        DocumentModelList parentList = ToutaticeDocumentHelper.getParentList(session, document, webIdExists, true, true);
        
        if(CollectionUtils.isNotEmpty(parentList)){
            DocumentModel parentWithWebId = parentList.get(0);
            parentWebId = (String) parentWithWebId.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
        }
        
        return parentWebId;
    }

}
