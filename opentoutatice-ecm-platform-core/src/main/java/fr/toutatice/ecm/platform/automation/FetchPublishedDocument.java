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

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.service.url.NoSuchDocumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

@Operation(id = FetchPublishedDocument.ID, category = Constants.CAT_FETCH, label = "PublishedDocument", description = "Fetch the published document from the repository given its reference (path or UID). Find the proxy document associated with the document passed as parameter 'value'. If the document is not published (no proxy exists) 404 http value (not found) is returned. The document will become the input of the next operation.")
public class FetchPublishedDocument {

	public static final String ID = "Document.FetchPublished";

	private static final Log log = LogFactory.getLog(FetchPublishedDocument.class);

    @Context
	protected CoreSession session;

	@Param(name = "value", required = true)
	protected DocumentModel value;

	@OperationMethod
	public DocumentModel run() throws Exception {

		// un document est publié quand il possède un proxy (publication locale)
		DocumentModel publishedDocument = ToutaticeDocumentHelper.getProxy(session, value, SecurityConstants.READ);

		if (null == publishedDocument) {
			// le document n'est pas en ligne
			throw new NoSuchDocumentException(value.getPathAsString());
		}
		
		return publishedDocument;
	}
	
}
