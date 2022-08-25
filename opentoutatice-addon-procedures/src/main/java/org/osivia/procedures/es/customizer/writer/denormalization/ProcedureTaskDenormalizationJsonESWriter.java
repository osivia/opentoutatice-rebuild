/**
 * 
 */
package org.osivia.procedures.es.customizer.writer.denormalization;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.osivia.procedures.es.customizer.ESCustomizerConstants;
import org.osivia.procedures.es.customizer.ProcedureDenormalizationHelper;

import com.fasterxml.jackson.core.JsonGenerator;

import fr.toutatice.ecm.es.customizer.writers.denormalization.AbstractDenormalizationJsonESWriter;


/**
 * @author david
 *
 */
public class ProcedureTaskDenormalizationJsonESWriter extends AbstractDenormalizationJsonESWriter {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(DocumentModel doc) {
    	return doc != null && StringUtils.equals(TaskConstants.TASK_TYPE_NAME, doc.getType());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void denormalizeDoc(JsonGenerator jg, DocumentModel taskDoc, String[] schemas, Map<String, String> contextParameters) throws IOException {
        DocumentModel pi = ProcedureDenormalizationHelper.getInstance().getProcedureInstanceOfTask(super.session, taskDoc);
        if(pi != null){
            jg.writeFieldName(ESCustomizerConstants.PI_IN_TASK_KEY);
            this.jsonESWriter.writeESDocument(jg, pi, pi.getSchemas(), contextParameters);
        }
    }

}
