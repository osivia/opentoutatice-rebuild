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

import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProviderService;


/**
 * @author david chevrier
 *
 */
@Operation(id = FetchExtendedDocumentInfos.ID, category = Constants.CAT_FETCH, label = "Fetch extended document informations",
description = "Fetch peculiar informations about the given document (used by Portal).")
public class FetchExtendedDocumentInfos {
    
    /** Logger. */
    private final static Log log = LogFactory.getLog(FetchExtendedDocumentInfos.class); 
    
    /** Operation id */
    public static final String ID = "Document.FetchExtendedDocInfos";
    
    /** Session */
    @Context
    protected CoreSession session;
    
    /** Id of document: path, id, webId */
    @Param(name = "path", required = false)
    protected DocumentModel document;
    
    @OperationMethod
    public Blob run() throws Exception {
        // For Trace logs
        long begin = System.currentTimeMillis();
        if(log.isTraceEnabled()){
            log.trace(" ID: " + this.document.getPathAsString());
        }
        
        JSONArray rowDocInfos= new JSONArray();
        JSONObject docInfos = new JSONObject();
        
        DocumentInformationsProviderService fetchInfosService = Framework.getService(DocumentInformationsProviderService.class);
        if (fetchInfosService != null) {
            Map<String, Object> infos = fetchInfosService.fetchAllExtendedInfos(this.session, this.document);
            docInfos.accumulateAll(infos);
        }
        
        rowDocInfos.add(docInfos);
        
        if(log.isTraceEnabled()){
            long end = System.currentTimeMillis();
            log.trace(" Ended: " + String.valueOf(end - begin) + " ms ======= \r\n");
        }
        
        return new StringBlob(rowDocInfos.toString(), "application/json");
    }

}
