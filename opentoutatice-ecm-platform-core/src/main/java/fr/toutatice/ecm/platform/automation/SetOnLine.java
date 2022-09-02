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

import org.nuxeo.ecm.automation.*;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.api.Framework;

@Operation(id = SetOnLine.ID, category = Constants.CAT_DOCUMENT, label = "Set on line a document", description = "Set on line a document (publish locally, acl copy).")
public class SetOnLine {
	
	public static final String ID = "Document.SetOnLineOperation";
	public static final String CHAIN_ID = "Document.SetOnLineChain";
	public static final String CHAIN_LOG_ID = "Log";
	
	@Context
	protected CoreSession session;

	@OperationMethod
	public DocumentModel run(DocumentModel doc) throws Exception {
		UnrestrictedSetOnLineRunner setOnLineRunner = new UnrestrictedSetOnLineRunner(session, doc);
		setOnLineRunner.runUnrestricted();	
		
		logAudit(doc);
		
		return setOnLineRunner.getDocument();
	}

	private static class UnrestrictedSetOnLineRunner extends UnrestrictedSessionRunner {
		
		private DocumentModel document;
		
		public DocumentModel getDocument(){
			return this.document;
		}
		
		public UnrestrictedSetOnLineRunner(CoreSession session, DocumentModel document){
			super(session);
			this.document = document;
		}
		
		@Override
		public void run() {
			
			OperationChain onLineChain = new OperationChain(CHAIN_ID);
			
			OperationParameters publishOpParam = new OperationParameters("Document.TTCPPublish");
			DocumentModel parentDoc = this.session.getParentDocument(this.document.getRef());
			publishOpParam.set("target", parentDoc);
			publishOpParam.set("override", true);
			onLineChain.add(publishOpParam);
			
			OperationParameters copyAclsOpParam = new OperationParameters("Document.CopyACLs");
			copyAclsOpParam.set("the source document", this.document);
			copyAclsOpParam.set("Copy all ACLs", true);
			copyAclsOpParam.set("Overwrite", true);	
			onLineChain.add(copyAclsOpParam);

            try {
                runChain(this.session, this.document, onLineChain);
            } catch (OperationException e) {
                throw new RuntimeException(e);
            }
        }
		
	}
	
	/*
	 * ToutaticeOperationHelper n'est pas utilisé car la chaîne doit être enregistrée
	 * pour référencement par id.
	 */
	private static void runChain(CoreSession session, DocumentModel doc, OperationChain logChain) throws OperationException {
		AutomationService automationService = Framework.getService(AutomationService.class);
		OperationContext context = new OperationContext(session);
		context.setInput(doc);
		automationService.run(context, logChain);
	}
	
	/*
	 * Déporté pour être exécuté dans la session utilisateur
	 */
	private void logAudit(DocumentModel doc) throws InvalidChainException, OperationException, Exception {
		OperationChain logChain = new OperationChain(CHAIN_LOG_ID);
		OperationParameters logOpParam = new OperationParameters("Audit.Log");
		logOpParam.set("event", "Mise en ligne");
		logOpParam.set("category", "Automation");
		logOpParam.set("comment", "Version " + doc.getVersionLabel());
		logChain.add(logOpParam);
		runChain(session, doc, logChain);

	}

}
