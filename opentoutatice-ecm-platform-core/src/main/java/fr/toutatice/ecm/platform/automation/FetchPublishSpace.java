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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;

@Operation(id = FetchPublishSpace.ID, category = Constants.CAT_FETCH, label = "Fetch the plubish space", description = "Find among the parents list the publish space document. Return the parent publish space document or null if no-one is found or if this one is not online")
public class FetchPublishSpace {
	public static final String ID = "Document.FetchPublishSpace";

	private static final Log log = LogFactory.getLog(FetchPublishSpace.class);

	@Context
	protected CoreSession coreSession;

	@Param(name = "value", required = true)
	protected DocumentModel value;

	@OperationMethod
	public Object run() throws Exception {
		DocumentModel publishSpaceDoc = null;

		if (null != value) {
			// vérifier que le document courant n'est pas lui même un espace de
			// publication
			Filter filter = new PublishSpaceDocumentFilter();
			if (filter.accept(value)) {
				publishSpaceDoc = value;
			} else {
				/*
				 * rechercher l'espace de publication parent (opération réalisée
				 * en mode restricted afin de s'assurer que l'utilisateur
				 * connecté possède bien une visibilité sur les parents)
				 */
				DocumentModelList parentList = ToutaticeDocumentHelper.getParentList(coreSession, value, filter, true);
				if (null != parentList && parentList.size() > 0) {
					// prendre le parent direct
					publishSpaceDoc = parentList.get(0);

					// vérifier les droits
					if (!coreSession.hasPermission(publishSpaceDoc.getRef(), SecurityConstants.READ)) {
						throw new DocumentSecurityException("");
					}
				} else {
					throw new DocumentNotFoundException(value.getPathAsString());
				}
			}
		}

		return publishSpaceDoc;
	}

	private class PublishSpaceDocumentFilter implements Filter {

		private static final long serialVersionUID = 3207718135474475149L;

		@Override
		public boolean accept(DocumentModel document) {
			boolean status = false;

    		try {
    			status = document.hasFacet(ToutaticeNuxeoStudioConst.CST_DOC_FACET_TTC_PUBLISH_SPACE);
    			
    			if (true == status) {
    				// vérifier que le folder est en ligne (possède un proxy)
    				status = (null != ToutaticeDocumentHelper.getProxy(coreSession, document, null));
    			}
    		} catch (Exception e) {
    			log.error("Failed to filter the publish space document, error: " + e.getMessage());
    			status = false;
    		}

    		return status;
		}
		
    }
    
}
