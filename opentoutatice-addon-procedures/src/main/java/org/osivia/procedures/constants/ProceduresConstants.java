package org.osivia.procedures.constants;

/**
 * @author david
 *
 */
public interface ProceduresConstants {

	String GENERICMODEL_ID = "generic-model";
	String PI_CONTAINER_TYPE = "ProceduresInstancesContainer";
	String PI_CONTAINER_PATH = "/default-domain/procedures-models/procedures-instances";
	String PI_TYPE = "ProcedureInstance";
	String PROCEDURE_TYPE = "Procedure";
    String RECORD_TYPE = "Record";
    String RECORD_MODEL_TYPE = "RecordFolder";

    String PROCEDURE_DEFINITIONS_XPATH = "pcd:globalVariablesDefinitions";
	String PI_VALUES_XPATH = "pi:globalVariablesValues";
    String RCD_VALUES_XPATH = "rcd:globalVariablesValues";
	String PI_ENTRY_KEY = "name";
	String TASK_ENTRY_KEY = "key";
	String ENTRY_VALUE = "value";

	String TEST_PROCEDURE_PATH = "/default-domain/procedures-models";
	
	String DEFAULT_REPOSITORY_NAME = "default";

}
