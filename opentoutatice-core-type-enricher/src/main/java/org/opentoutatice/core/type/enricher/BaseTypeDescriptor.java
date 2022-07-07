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
package org.opentoutatice.core.type.enricher;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;


/**
 * @author david
 *
 */
@XObject("basettc")
public class BaseTypeDescriptor implements Serializable {

    private static final long serialVersionUID = -905795010892961319L;
    
    @XNode("doctype")
    DocumentTypeDescriptor docTypeDescriptor;
    
    public DocumentTypeDescriptor getBaseDocTypeDescriptor(){
        return docTypeDescriptor;
    }

}
