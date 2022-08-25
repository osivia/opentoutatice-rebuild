/**
 * 
 */
package org.osivia.procedures.es.customizer.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.osivia.procedures.constants.ProceduresConstants;
import org.osivia.procedures.es.customizer.writer.helper.DenormalizationJsonESWriterHelper;

import com.fasterxml.jackson.core.JsonGenerator;

import fr.toutatice.ecm.es.customizer.writers.api.AbstractCustomJsonESWriter;


/**
 * @author david
 *
 */
public class TaskJsonESWriter extends AbstractCustomJsonESWriter {

    /**
     * Constructor.
     */
    public TaskJsonESWriter() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(DocumentModel doc) {
        return doc != null && TaskConstants.TASK_TYPE_NAME.equals(doc.getType());
    }

    /**
     * We remove HiddenInNavigation facet (to be able to query on Task in FO)
     * and mp keys / values of nt:task_variables.
     */
    @Override
    public void writeData(JsonGenerator jg, DocumentModel doc, String[] schemas, Map<String, String> contextParameters) throws IOException {
        // Remove HiddenInNavigation.
        doc.removeFacet(FacetNames.HIDDEN_IN_NAVIGATION);
        
        jg.writeArrayFieldStart("ecm:mixinType");
        for (String facet : doc.getFacets()) {
            jg.writeString(facet);
        }
        jg.writeEndArray();
        
        // Adapt keys / values.
        DenormalizationJsonESWriterHelper.mapKeyValue(jg, doc, TaskConstants.TASK_VARIABLES_PROPERTY_NAME, 
                ProceduresConstants.TASK_ENTRY_KEY, ProceduresConstants.ENTRY_VALUE);
    }

}
