/**
 * 
 */
package org.opentoutatice.core.type.enricher;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;


/**
 * Add defined schemas, facets on all document types (basettc extension point of CustomizeTypeService)
 * excepted on peculiar documents (excludedRules extension point of CustomizeTypeService).
 * 
 * @author david
 *
 */
public class SchemaManagerEnricher {

    /** Logger. */
    private static final Log log = LogFactory.getLog(SchemaManagerEnricher.class);

    private SchemaManager schemaManager;

    /** Description of generic schemas and facets add. */
    private DocumentTypeDescriptor baseDocTypeDescriptor;

    /** Types to exclude from generic treatment. */
    protected List<String> excludedTypes;
    /** Facets excluding types. */
    protected List<String> facetsExcludingTypes;

    /**
     * Constructor.
     */
    public SchemaManagerEnricher() {
        super();

        // Rules
        this.excludedTypes = new ArrayList<>();
        this.facetsExcludingTypes = new ArrayList<>();
    }

    public SchemaManager getSchemaManager() {
        if (this.schemaManager == null) {
            this.schemaManager = Framework.getService(SchemaManager.class);
        }
        return this.schemaManager;
    }


    /**
     * @return the baseDocTypeDescriptor
     */
    public DocumentTypeDescriptor getBaseDocTypeDescriptor() {
        return baseDocTypeDescriptor;
    }


    /**
     * @param baseDocTypeDescriptor the baseDocTypeDescriptor to set
     */
    public void setBaseDocTypeDescriptor(DocumentTypeDescriptor baseDocTypeDescriptor) {
        this.baseDocTypeDescriptor = baseDocTypeDescriptor;
    }


    /**
     * @return the excludedTypes
     */
    public List<String> getExcludedTypes() {
        return excludedTypes;
    }

    /**
     * @param excludedTypes the excludedTypes to set
     */
    public void setExcludedTypes(List<String> excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

    /**
     * @return the facetsExcludingTypes
     */
    public List<String> getFacetsExcludingTypes() {
        return facetsExcludingTypes;
    }

    /**
     * @param facetsExcludingTypes the facetsExcludingTypes to set
     */
    public void setFacetsExcludingTypes(List<String> facetsExcludingTypes) {
        this.facetsExcludingTypes = facetsExcludingTypes;
    }

    public void enrichTypes() {
        final long begin = System.currentTimeMillis();
        
        ListIterator<DocumentTypeDescriptor> allListIt = getAllRegisteredDocumentTypes().listIterator();

        while (allListIt.hasNext()) {
            DocumentTypeDescriptor typeDescriptor = allListIt.next();

            // Type rule
            boolean toExcludeFromEnriching = this.excludedTypes.contains(typeDescriptor.name);
            // Facets rule (typeDescriptor.facets is null here when document type is first defined as extending other document type)
            if (typeDescriptor.facets != null) {
                toExcludeFromEnriching |= CollectionUtils.containsAny(Arrays.asList(typeDescriptor.facets), this.facetsExcludingTypes);
            }

            if (toExcludeFromEnriching) {
                if (log.isDebugEnabled()) {
                    log.debug("Not enriched: " + typeDescriptor.name);
                }
            } else {
                allListIt.set(typeDescriptor.merge(this.baseDocTypeDescriptor));

                if (log.isDebugEnabled()) {
                    log.debug("Enriched: " + typeDescriptor.name);
                }
            }
        }


        if (log.isDebugEnabled()) {
            final long part = System.currentTimeMillis();
            log.debug("#enrichTypes: " + String.valueOf(part - begin) + " ms");
        }
        
    }

    protected List<DocumentTypeDescriptor> getAllRegisteredDocumentTypes() {
        List<DocumentTypeDescriptor> dtds = null;

        try {
            dtds = (List<DocumentTypeDescriptor>) getAllDocumentTypes().get(getSchemaManager());
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new EnricherTypeException("Unable to get enriched types: ", e);
        }

        return dtds;
    }

    protected void setAllRegisteredDocumentTypes(List<DocumentTypeDescriptor> enrichedTypes) {
        try {
            Field docTypesField = getAllDocumentTypes();
            docTypesField.set(getSchemaManager(), enrichedTypes);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new EnricherTypeException("Unable to register enriched types: ", e);
        }
    }

    private Field getAllDocumentTypes() throws NoSuchFieldException {
        Field docTypesField = getSchemaManager().getClass().getDeclaredField("allDocumentTypes");
        docTypesField.setAccessible(true);
        return docTypesField;
    }

}
