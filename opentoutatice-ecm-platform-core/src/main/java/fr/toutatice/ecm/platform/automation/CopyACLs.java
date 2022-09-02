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

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Operation(id = CopyACLs.ID, 
		category = Constants.CAT_DOCUMENT, 
		label = "Copy the ACLs", 
		description = "Copy the ACLs from one source document to the input document. Return the input document.")
public class CopyACLs {
    public static final String ID = "Document.CopyACLs";

    @Context
    protected CoreSession session;
    
    @Param(name = "the source document", required = true)
    protected DocumentModel srcDoc;

    @Param(name = "ACL name list (comma separated)", required = false)
    protected String ACLnames;

    @Param(name = "Copy all ACLs", required = false)
    protected boolean doCopyAll = false;

    @Param(name = "Overwrite", required = false)
    protected boolean doOverwrite = false;

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
    	if (null != srcDoc) {
    		copyACP(doc.getRef());
    	}
    	
    	return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) throws Exception {
    	if (null != srcDoc) {
    		copyACP(docRef);    	
    	}
    	
    	return session.getDocument(docRef);
    }
    
    @SuppressWarnings("unchecked")
    private void copyACP(DocumentRef docRef) {
		List<String> aclsToCopyList = Collections.emptyList();
    	
		// get the ACP from the source document
		ACP srcACP = session.getACP(srcDoc.getRef());
		
		// slurp the ACLs to copy
		if (!doCopyAll && StringUtils.isNotBlank(ACLnames)) {
			aclsToCopyList = Arrays.asList(ACLnames.split(","));
		}
		
		// copy the ACLs
		ACP dstACP = new ACPImpl();
		for (ACL srcAcl : srcACP.getACLs()) {
			if ( doCopyAll ||
				(!doCopyAll && aclsToCopyList.contains(srcAcl.getName())) ) {
				dstACP.addACL(srcAcl);
			}
		}
		
		// save new ACP
		session.setACP(docRef, dstACP, doOverwrite);
    }

}
