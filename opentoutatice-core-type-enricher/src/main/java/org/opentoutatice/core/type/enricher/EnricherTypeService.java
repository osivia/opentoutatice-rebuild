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
 */
package org.opentoutatice.core.type.enricher;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.Extension;

/**
 * @author david
 * 
 */
public class EnricherTypeService extends TypeService {

    private static final Log log = LogFactory.getLog(EnricherTypeService.class);

    public static final String BASE_TYPE_EXT_POINT = "basettc";
    public static final String EXCLUDED_RULES_EXT_POINT = "excludedRules";

    /** Schema manager enricher. */
    private SchemaManagerEnricher schemaManagerEnricher;

    /** Description of generic schemas and facets add. */
    private DocumentTypeDescriptor baseDocTypeDescriptor;
    /** Rules excluding peculiar documents of generic treatment. */
    private ExclusionRulesDescriptor rulesDescriptor;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        this.schemaManagerEnricher = new SchemaManagerEnricher();
    }

    @Override
    public void registerExtension(Extension extension) {
        // Default registration
        super.registerExtension(extension);

        String extensionPoint = extension.getExtensionPoint();
        // Generic schema and facets to add
        if (BASE_TYPE_EXT_POINT.equals(extensionPoint)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                if (this.baseDocTypeDescriptor == null) {
                    this.baseDocTypeDescriptor = (DocumentTypeDescriptor) contrib;
                } else {
                    this.baseDocTypeDescriptor.merge((DocumentTypeDescriptor) contrib);
                }

                if (this.baseDocTypeDescriptor != null) {
                    this.schemaManagerEnricher.setBaseDocTypeDescriptor(this.baseDocTypeDescriptor);
                }
            }
        }
        // Exclusion rules: only one configuration for the moment
        if (EXCLUDED_RULES_EXT_POINT.equals(extensionPoint)) {
            Object contrib = extension.getContributions()[0];
            if (this.rulesDescriptor == null) {
                this.rulesDescriptor = (ExclusionRulesDescriptor) contrib;

                // Fill Enricher delegate attributes
                this.schemaManagerEnricher.setExcludedTypes(Arrays.asList(this.rulesDescriptor.getTypes()));
                this.schemaManagerEnricher.setFacetsExcludingTypes(Arrays.asList(this.rulesDescriptor.getFacets()));
            }
        }

    }

    
    @Override
    public void start(ComponentContext context) {
        long begin = System.currentTimeMillis();
        
        if(log.isInfoEnabled()) {
        	log.info("Enriching doctypes setting toutatice schema ...");
        }

        // All types (descriptors) are registered here: enrich
        this.schemaManagerEnricher.enrichTypes();
        // Computing
        super.start(context);

        if (log.isDebugEnabled()) {
            long end = System.currentTimeMillis();
            log.debug("enrichTypes + flushPendingsRegistration: " + String.valueOf(end - begin) + " ms");
        }
        
        if(log.isInfoEnabled()) {
        	log.info("Doctypes enriched with toutatice schema.");
        }
    }
    

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        this.schemaManagerEnricher = null;
    }

}
