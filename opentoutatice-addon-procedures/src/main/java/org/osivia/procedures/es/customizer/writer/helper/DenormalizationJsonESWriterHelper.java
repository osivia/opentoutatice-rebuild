/**
 *
 */
package org.osivia.procedures.es.customizer.writer.helper;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.schema.utils.DateParser;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * @author david
 *
 */
public class DenormalizationJsonESWriterHelper {

	public static Pattern COMPLEX_PROPERTY_PATTERN = Pattern.compile("^[\\[\\{](.+)[\\]\\}]$");
	public static Pattern DATE_PATTERN = Pattern.compile("^([0-2][0-9]||3[0-1])/(0[0-9]||1[0-2])/([0-9][0-9])?[0-9][0-9]$");

	public static final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");

	/**
	 * Utility class.
	 */
	private DenormalizationJsonESWriterHelper() {
		super();
	}

	/**
	 * listPropXPath refers a ListProperty of document. List is of the form:
	 * [{entryKey: a, entryValue: b}, {entryKey: c, entryValue: d}, .. ] This method
	 * writes the list as a map: {a : b, c : d, ...}.
	 *
	 * @param jg
	 * @param doc
	 * @param listPropXPath
	 * @param entryKey
	 * @param entryValue
	 * @return
	 * @throws JsonGenerationException
	 * @throws IOException
	 */
	public static JsonGenerator mapKeyValue(JsonGenerator jg, DocumentModel doc, String listPropXPath,
            String entryKey, String entryValue) throws JsonGenerationException, IOException {

        ListProperty valuesProp = (ListProperty) doc.getProperty(listPropXPath);

        if ((valuesProp != null) && !valuesProp.isEmpty()) {
            jg.writeFieldName(listPropXPath);
            jg.writeStartObject();

            for (Property valueProp : valuesProp) {
                String name = (String) valueProp.get(entryKey).getValue();
                String value = (String) valueProp.get(entryValue).getValue();


                if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                    jg.writeStringField(name, value);
                }
            }

            jg.writeEndObject();
        }

        return jg;
    }

	public static JsonGenerator mapKeyValueAsJson(JsonGenerator jg, String customProperty, DocumentModel doc, String listPropXPath, String entryKey,
			String entryValue) throws JsonGenerationException, IOException {

		ListProperty valuesProp = (ListProperty) doc.getProperty(listPropXPath);

		if (CollectionUtils.isNotEmpty(valuesProp)) {

            jg.writeFieldName(customProperty);
			jg.writeStartObject();

			for (Property valueProp : valuesProp) {
				String name = (String) valueProp.get(entryKey).getValue();
				String value = (String) valueProp.get(entryValue).getValue();

				if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {

					Matcher matcher = COMPLEX_PROPERTY_PATTERN.matcher(value);
					if (matcher.matches()) {
						ObjectMapper mapper = new ObjectMapper();
						BaseJsonNode jsonNode = mapper.readValue(value, BaseJsonNode.class);

						jg.writeFieldName(name);
						jsonNode.serialize(jg, null);
					} else {
						Matcher dateMatcher = DATE_PATTERN.matcher(value);
						if(dateMatcher.matches()) {
							String initialValue = value;
							try {
								value = DateParser.formatW3CDateTime(format.parse(value));
							} catch (ParseException e) {
								value = initialValue;
							}
						}

						jg.writeStringField(name, value);
					}
				}
			}

			jg.writeEndObject();

		}

		return jg;
	}

}
