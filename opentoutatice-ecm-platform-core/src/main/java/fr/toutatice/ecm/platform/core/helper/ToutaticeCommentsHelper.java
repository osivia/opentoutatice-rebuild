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
package fr.toutatice.ecm.platform.core.helper;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author David Chevrier
 */
public class ToutaticeCommentsHelper {
    
    private ToutaticeCommentsHelper(){}

    public static Map<DocumentModel, List<DocumentModel>> getProxyComments(DocumentModel proxy) {
        Map<DocumentModel, List<DocumentModel>> comments = new HashMap<DocumentModel, List<DocumentModel>>();
        CommentableDocument commentableDoc = proxy.getAdapter(CommentableDocument.class);
        if(commentableDoc != null) { // Nullsafe
	        List<DocumentModel> rootComments = commentableDoc.getComments();
	        if (null != rootComments && 0 < rootComments.size()) {
	        	for(DocumentModel rootComment : rootComments){
	        		List<DocumentModel> commentsThread = commentableDoc.getComments(rootComment);
	        		comments.put(rootComment, commentsThread);
	        	}
	        }
        }
        return comments;
    }

    public static void setComments(CoreSession session, DocumentModel document, Map<DocumentModel, List<DocumentModel>> comments) {
        CommentableDocument commentableDoc = document.getAdapter(CommentableDocument.class);
        for(DocumentModel rootComment : comments.keySet()){
            commentableDoc.addComment(rootComment);
            for(DocumentModel comment : comments.get(rootComment)){
                commentableDoc.addComment(rootComment,  comment);
            }
        }
    }
    
    

}
