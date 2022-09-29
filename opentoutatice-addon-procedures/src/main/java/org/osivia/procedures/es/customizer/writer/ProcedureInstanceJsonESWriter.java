/**
 * 
 */
package org.osivia.procedures.es.customizer.writer;

import java.io.IOException;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.osivia.procedures.constants.ProceduresConstants;
import org.osivia.procedures.es.customizer.writer.helper.DenormalizationJsonESWriterHelper;

import com.fasterxml.jackson.core.JsonGenerator;

import fr.toutatice.ecm.es.customizer.writers.api.AbstractCustomJsonESWriter;


/**
 * @author david
 *
 */
public class ProcedureInstanceJsonESWriter extends AbstractCustomJsonESWriter {

    /**
     * Default constructor.
     */
    public ProcedureInstanceJsonESWriter() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(DocumentModel doc) {
        return doc != null && ProceduresConstants.PI_TYPE.equals(doc.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeData(JsonGenerator jg, DocumentModel pi, String[] schemas, Map<String, String> contextParameters) throws IOException {

        // TODO problème de dénoramlisation, la variable pi:globalVariablesBalues est déjà dans le mapping

        // Custom name /value
        //DenormalizationJsonESWriterHelper.mapKeyValue(jg, pi, ProceduresConstants.PI_VALUES_XPATH,
        //        ProceduresConstants.PI_ENTRY_KEY, ProceduresConstants.ENTRY_VALUE);
        // Custom name / value as Json
        //DenormalizationJsonESWriterHelper.mapKeyValueAsJson(jg, "pi:data", pi, ProceduresConstants.PI_VALUES_XPATH,
        //        ProceduresConstants.PI_ENTRY_KEY, ProceduresConstants.ENTRY_VALUE);
    }

}
