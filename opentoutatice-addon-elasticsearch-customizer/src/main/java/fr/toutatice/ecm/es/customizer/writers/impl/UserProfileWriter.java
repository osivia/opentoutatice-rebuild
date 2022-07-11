/**
 * 
 */
package fr.toutatice.ecm.es.customizer.writers.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.ldap.LDAPDirectory;
import org.nuxeo.ecm.directory.ldap.LDAPSession;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

import fr.toutatice.ecm.es.customizer.writers.api.AbstractCustomJsonESWriter;

/**
 * Class for User Profile denormalization :
 * - the displayname is set to the dc:title
 * - the login is set to ttc_userprofile:login
 * - workspaces Ids (webc:url) are computed to ttc_userprofile:workspaces 
 * 
 * @author Loïc Billon
 *	
 */
public class UserProfileWriter extends AbstractCustomJsonESWriter {

	private LDAPSession session;

	/* (non-Javadoc)
	 * @see fr.toutatice.ecm.es.customizer.writers.api.ICustomJsonESWriter#accept(org.nuxeo.ecm.core.api.DocumentModel)
	 */
	@Override
	public boolean accept(DocumentModel doc) {
		return doc.getType().equals("UserProfile");
	}

	/* (non-Javadoc)
	 * @see fr.toutatice.ecm.es.customizer.writers.api.AbstractCustomJsonESWriter#writeData(org.codehaus.jackson.JsonGenerator, org.nuxeo.ecm.core.api.DocumentModel, java.lang.String[], java.util.Map)
	 */
	@Override
	public void writeData(JsonGenerator jg, DocumentModel doc, String[] schemas, Map<String, String> contextParameters)
			throws IOException {
		
		// Get the original login in the ACLs of the document
		String login = null;
		for(ACL acl : doc.getACP().getACLs()) {
			for(ACE ace : acl.getACEs()) {
				if(ace.getPermission().equals("Everything") && ace.isGranted()) {
					login = ace.getUsername();
				}
			}
		}
		
		if(login != null) {
			
			DocumentModel entry = getLdapEntry(login);
			
			if(entry != null) {

				Serializable firstName = entry.getPropertyValue("firstName");
				Serializable lastName = entry.getPropertyValue("lastName");
				
				// Compute display Name
				String displayName = null;
				if(firstName != null && lastName != null) {
					displayName = firstName.toString() + " " + lastName.toString();
				}
				else if(lastName != null) {
					displayName = lastName.toString();
				}
				
				// Compute workspace lists
				Set<String> workspaces = new HashSet<String>();
				
				List<String> groups = (List<String>) entry.getPropertyValue("groups");
				for(String group : groups) {
				
					String[] split = group.split("_");
					workspaces.add(split[0]);
				}
				
				
				jg.writeStringField("dc:title", displayName);
				jg.writeStringField("ttc_userprofile:login", login);
				jg.writeArrayFieldStart("ttc_userprofile:workspaces");
				for(String workspace : workspaces) {
					jg.writeString(workspace);
				}
				jg.writeEndArray();
			}
		}
		
	}

	/**
	 * This method get a fresh user from ldap, recycling a dedicated connexion to ldap.
	 * If ldap client is unbinded, try to create a new one.
	 * 
	 * @param login str
	 * @return document ldap entry
	 */
	private DocumentModel getLdapEntry(String login) {
		DocumentModel entry = null;
		if(session != null) {
//			try {
				entry = session.getEntryFromSource(login, false);
//			}
//			catch(ClientException e) {
//				session = null;
//			}
		}
		
		if(session == null) {
			
			DirectoryService service = Framework.getService(DirectoryService.class);
			LDAPDirectory directory = (LDAPDirectory) service.getDirectory("userLdapDirectory");
			
			session = (LDAPSession) directory.getSession();
			
			entry = session.getEntryFromSource(login,false);

		}
		
		return entry;
	}

}
