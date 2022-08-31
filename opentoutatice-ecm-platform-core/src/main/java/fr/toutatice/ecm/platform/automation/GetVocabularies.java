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
package fr.toutatice.ecm.platform.automation;

import java.io.Serializable;
import java.net.URLEncoder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDirectoryMngtHelper;

@Operation(id = GetVocabularies.ID, 
category = Constants.CAT_SERVICES,
label = "Get Vocabularies", 
since = "5.4",
description = "Vocabularies are serialized using JSON and returned in a Blob.")
public class GetVocabularies {
	public static final String ID = "Document.GetVocabularies";

	private static final Log log = LogFactory.getLog(GetVocabularies.class);
	private static final Long NOT_OBSOLETE = new Long(0);

	@Param(name = "vocabularies", required = true)
	protected String vocabularies;

	@Param(name = "locale", required = false)
	protected String locale;

	private ArrayList<String> indexVoc;
	private ArrayList<VocabularyEntry> listValVoc;
	private HashMap<String, ArrayList<VocabularyEntry>> listVoc;

	@OperationMethod
	public Blob run() throws Exception 
	{
		String voc;
		DocumentModelList entries = null;

		Locale localeChoisie = null;
		if (StringUtils.isNotBlank(locale)) {
			localeChoisie = new Locale(locale);
		} else {
			localeChoisie = Locale.FRENCH;
		}

		// Alimentation des structures de données à partir des Directories Nuxeo
		listVoc = new HashMap<String, ArrayList<VocabularyEntry>>();
		indexVoc = new ArrayList<String>();
		StringTokenizer parVocToken = new StringTokenizer(vocabularies, ";");
		while (parVocToken.hasMoreTokens()) {
			voc = parVocToken.nextToken();
			indexVoc.add(voc);
			entries = ToutaticeDirectoryMngtHelper.instance().getEntries(voc);
			listValVoc = new ArrayList<VocabularyEntry>();
			for (DocumentModel entry : entries) {
				VocabularyEntry ve = new VocabularyEntry(voc, localeChoisie, entry);
				if (ve.getObsolete().equals(NOT_OBSOLETE)) {
					listValVoc.add(ve);
				}
			}
			Collections.sort(listValVoc, new VocabularyEntryComparator());
			listVoc.put(voc, listValVoc);
		}

		// Parcours des structures de données et construction du flux JSON
		JSONArray rows = getChildren(null, 0);

		if (log.isDebugEnabled()) {
			log.debug("JSON: " + rows);
		}

		return new StringBlob(rows.toString(), "application/json");
	}

	private JSONArray getChildren(String key, int i) {
		JSONArray rows = new JSONArray();
		String voc = "";
		if (i < indexVoc.size()) {
			voc = indexVoc.get(i);
			ArrayList<VocabularyEntry> entries = listVoc.get(voc);
			i++;
			for (VocabularyEntry entry : entries) {
				String valkey = entry.getTitle();
				String vali18n = entry.getLabel();
				String valparent = entry.getParent();
				if ((key == null) || key.equalsIgnoreCase(valparent)) {
					// remplir l'objet JSON
					JSONObject obj = new JSONObject();
					obj.element("key", valkey);
					obj.element("value", vali18n);
					obj.element("parent", valparent);
					if(i != indexVoc.size()) {
						obj.element("children", getChildren(valkey, i));
					}
					rows.add(obj);
				}
			}
		}

		return rows;
	}

	private class VocabularyEntry {

		private String title;
		private String label;
		private String parent;
		private Long ordering;
		private Long obsolete;

		public VocabularyEntry(String vocabulary, Locale locale, DocumentModel entry) {
			try {
				String schema = getDirectorySchema(entry);
				this.ordering = (Long) entry.getProperty(schema, "ordering");
				this.obsolete = (Long) entry.getProperty(schema, "obsolete");
				this.title = URLEncoder.encode(entry.getTitle(),"UTF-8");
				String localizedEntryLabel = ToutaticeDirectoryMngtHelper.instance().getDirectoryEntryLocalizedLabel(vocabulary, entry.getId(), locale);
				this.label = URLEncoder.encode(localizedEntryLabel,"UTF-8");
				try {
				    this.parent = (String) entry.getProperty(schema, "parent");
				} catch(Exception e){
				   // If schema does not contain parent field
				   this.parent = StringUtils.EMPTY;
				}
			} catch (Exception e) {
				log.error("Failed to instanciate VocabularyEntry, vocabulary:'" + vocabulary + "', entry:'" + entry.getId() + "', error:" + e.getMessage());
				//throw new ClientException(e);
			}
		}

		public String getTitle() {
			return title;
		}

		public String getLabel() {
			return label;
		}

		public String getParent() {
			return parent;
		}

		public Long getOrdering() {
			return ordering;
		}

		public Long getObsolete() {
			return obsolete;
		}

		private String getDirectorySchema(DocumentModel entry) {
			String schema = null;

			String[] schemaList = entry.getSchemas();
			if (null != schemaList && 0 < schemaList.length) {
				schema = schemaList[0];
			}

			return schema;
		}

		public String toString() {
			return "{parent:" + getParent() + ", title:" + getTitle() + ", label:" + getLabel() + ", ordering:" + getOrdering() + ", obsolete:" + !getObsolete().equals(NOT_OBSOLETE) + "}";
		}

	}

	private class VocabularyEntryComparator implements Comparator<VocabularyEntry>, Serializable {

		private static final long serialVersionUID = 3330267100994507692L;
		private Collator collator;

		public VocabularyEntryComparator() {
			this.collator = Collator.getInstance();
			this.collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
			this.collator.setStrength(Collator.TERTIARY);   
		}

		@Override
		public int compare(VocabularyEntry e1, VocabularyEntry e2) {
			int status = 0;

			status = e1.getOrdering().compareTo(e2.getOrdering());
			if (0 == status) {
				status = collator.compare(e1.getLabel(), e2.getLabel());
			}

			return status;
		}

	}

}
