/*
 * (C) Copyright 2016 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
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
 *   kle-helley
 */
package fr.toutatice.ecm.platform.automation.document;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.core.query.helper.ToutaticeQueryHelper;
import fr.toutatice.ecm.platform.service.url.ToutaticeWebIdHelper;

import javax.security.auth.login.LoginException;

@Operation(
		id = CreateDocument.ID,
		category = Constants.CAT_DOCUMENT,
		label = "Create a new document",
		description = "Create a new document in the input folder. If the 'name' parameter is not set, a new name will be derived from the document title, using the same "
				+ "naming strategy as Nuxeo when using the GUI (if the document has a title). This is the only difference between this operation and 'Document.Create', "
				+ "with the latter defaulting the name to 'Untitled'.")
public class CreateDocument extends AbstractDublinCoreDocumentUpdate {

	public static final String ID = "Document.TTCCreate";
	
	@Context
	protected CoreSession session;

	@Context
	protected PathSegmentService pathSegmentService;

	@Param(name = "type")
	protected String type;

	@Param(name = "name", required = false)
	protected String name;

	@Param(name = "properties", required = false)
	protected Properties properties;

	@OperationMethod(collector = DocumentModelCollector.class)
	public DocumentModel run(final DocumentModel parentDoc) throws Exception {
	    // Build name from title if any
		if (this.name == null) {
			if ((properties != null) && (properties.get(PROP_TITLE) != null)) {
			    this.name = pathSegmentService.generatePathSegment(properties.get(PROP_TITLE));
			} else {
			    this.name = "Untitled";
			}
		}
		
		// Test if webId exists
        if (parentDoc.hasSchema(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE) && this.properties != null) {
            String wId = this.properties.get(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
            if (StringUtils.isNotBlank(wId)) {
                DocumentModelList results = ToutaticeQueryHelper.queryUnrestricted(session, String.format(ToutaticeWebIdHelper.WEB_ID_QUERY, wId), 1);
                // Don't allow creation from Portal if webId already exists
                if (!results.isEmpty()) {
                    throw new RuntimeException("WebId: " + wId + " already exists.");
                }
            }
		}

		DocumentModel newDoc = this.session.createDocumentModel(parentDoc.getPathAsString(), this.name, this.type);

		if (this.properties != null) {
		    // Creates document taking DublinCore properties into account
		    newDoc = super.executeSplittingProperties(this.session, newDoc, this.properties, true);
		} else {
		    // Creates
		    newDoc = execute(this.session, newDoc, this.properties, true);
		}
		
		return newDoc;
	}

	@OperationMethod(collector = DocumentModelCollector.class)
	public DocumentModel run(final DocumentRef doc) throws Exception {
		return run(this.session.getDocument(doc));
	}
	
	 @Override
	    protected DocumentModel execute(CoreSession session, DocumentModel document, Properties properties, boolean save) throws IOException {
	     if( properties != null)
	        DocumentHelper.setProperties(session, document, properties);
	     return session.createDocument(document);
	    }
	
	/**
	 * Creates document setting Dublincore properties.
	 * 
	 * @param session
	 * @param document
	 * @param properties
	 * @param dublinCoreProperties
	 * @return document
	 * @throws ClientException
	 * @throws IOException
	 */
	@Override
    protected DocumentModel execute(CoreSession session, DocumentModel document, Properties properties, Properties dublinCoreProperties, boolean save) throws IOException, LoginException {
	    // Create document without given dublincore properties:
	    // DublinCoreListener sets them
	    DocumentHelper.setProperties(session, document, properties);
	    DocumentModel createDocument = session.createDocument(document);
	    
	    // Set dublincore properties and save silently to shortcut DublinCoreListener
	    DocumentHelper.setProperties(session, createDocument, dublinCoreProperties);
	    ToutaticeDocumentHelper.saveDocumentSilently(session, createDocument, false);
	    
	    return createDocument;
	}

}
