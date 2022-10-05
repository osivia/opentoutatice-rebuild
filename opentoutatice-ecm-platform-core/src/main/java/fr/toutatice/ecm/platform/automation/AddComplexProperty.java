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
 * lbillon
 */
package fr.toutatice.ecm.platform.automation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;

/**
 * Add an entry in a complex property defined in the xpath parameter. The value must be a list of params seperated with line separator ( \n).
 * 
 * @author lbillon
 */
@Operation(
        id = AddComplexProperty.ID,
        category = Constants.CAT_DOCUMENT,
        label = "Add Complex Property",
        description = "Create an entry in a complex property value on the input document. The property is specified using its xpath. The document is automatically saved if 'save' parameter is true. If you unset the 'save' you need to save it later using Save Document operation. Return the modified document.")
public class AddComplexProperty {

    public static final String ID = "Document.AddComplexProperty";

    @Context
    protected CoreSession session;

    @Param(name = "xpath")
    protected String xpath;

    @Param(name = "value")
    protected Properties value;

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {

        Property property = doc.getProperty(xpath);

        if (property != null) {
            Serializable value2 = property.getValue();

            if (value2 instanceof Serializable && value2 instanceof List<?>) {
                List<Map<String, Object>> complexList = (List<Map<String, Object>>) value2;

                complexList.add(new HashMap<String, Object>(value));

                doc.setPropertyValue(xpath, (Serializable) complexList);
            } else {
                throw new DocumentException("the value is not a Serializable List " + value);
            }

        } else {
            throw new DocumentException("no property with name " + property);
        }


        if (save) {
            doc = session.saveDocument(doc);
        }


        return doc;
    }

}
