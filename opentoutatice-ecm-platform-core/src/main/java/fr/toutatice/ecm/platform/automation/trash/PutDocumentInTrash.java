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
package fr.toutatice.ecm.platform.automation.trash;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;


/**
 * @author David Chevrier
 */
@Operation(id = PutDocumentInTrash.ID, category = Constants.CAT_DOCUMENT, label = "PutDocumentInTrash", description = "Put a document in trash.")
public class PutDocumentInTrash extends AbstractTrashOperation {
    
    public static final String ID= "Document.PutDocumentInTrash";
    
    @Context
    protected CoreSession session;
    
    @Param(name = "document", required = true)
    protected DocumentModel document;
    
    @OperationMethod
    public Object run() throws Exception {
        DocumentModelList docs = new DocumentModelListImpl(1);
        docs.add(this.document);
        
        return this.execute(session, docs);
    }
    
    @OperationMethod
    public Object run(DocumentModel document) throws Exception {
        this.document = document;
        return run();
    }

    @Override
    public void invoke(TrashService service, TrashInfo info) throws Exception {
        if (info.forbidden > 0) {
            throw new Exception("Can not put in trash!!");
        } else {
            // #3411 delete the local proxy if there
            DocumentModel proxy = ToutaticeDocumentHelper.getProxy(this.session, this.document, null, true);
            if (proxy != null) {
                this.session.removeDocument(proxy.getRef());
            }

            service.trashDocuments(info.docs);
        }

    }

}
