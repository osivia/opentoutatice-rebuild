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
 * mberhaut1
 */
package fr.toutatice.ecm.platform.automation.comments;

import java.io.IOException;
import java.security.Principal;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.JsDateJsonValueProcessor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;

@Operation(id = FetchCommentsOfDocument.ID, category = Constants.CAT_FETCH, label = "FetchCommentsOfDocument",
        description = "Fetches comments of a (commentable) document")
public class FetchCommentsOfDocument {

    public static final String ID = "Fetch.DocumentComments";

    private static final Log log = LogFactory.getLog(FetchCommentsOfDocument.class);

    public static final String COMMENT_TYPE = "Comment";
    public static final String THREAD_TYPE = "Thread";
    public static final String POST_TYPE = "Post";
    public static final String COMMENT_SCHEMA = "comment";
    public static final String POST_SCHEMA = "post";

    @Context
    CoreSession session;

    @Param(name = "commentableDoc", required = true)
    protected DocumentModel document;

    @OperationMethod
    public Object run() throws IOException {

        JSONArray commentsTree = new JSONArray();
        /*
         * Récupération du service de commentaires.
         */
        CommentableDocument commentableDoc = document.getAdapter(CommentableDocument.class);
        String schemaPrefix = COMMENT_SCHEMA;
        if (THREAD_TYPE.equals(document.getType())) {
            schemaPrefix = POST_SCHEMA;
        }
        List<DocumentModel> commentsRoots = commentableDoc.getComments();
        if (commentsRoots != null) {
            /*
             * Construction de la liste des fils de commentaires.
             */

            JsonConfig jsonConfig = new JsonConfig();
            jsonConfig.registerJsonValueProcessor(GregorianCalendar.class, new GregorianCalendarJsonValueProcessor());

            for (DocumentModel commentRoot : commentsRoots) {
                JSONObject jsonCommentRoot = new JSONObject();

                jsonCommentRoot.element("id", commentRoot.getId());
                jsonCommentRoot.element("path", commentRoot.getPathAsString());
                String author = (String) commentRoot.getProperty(schemaPrefix, "author");
                jsonCommentRoot.element("author", author);

                jsonCommentRoot.element("creationDate", commentRoot.getProperty(schemaPrefix, "creationDate"), jsonConfig);
                jsonCommentRoot.element("content", commentRoot.getProperty(schemaPrefix, "text"));
                jsonCommentRoot.element("modifiedDate", commentRoot.getProperty("dublincore", "modified"), jsonConfig);

                boolean canDelete = canDeleteComment(author, document);
                jsonCommentRoot.element("canDelete", canDelete);

                if (THREAD_TYPE.equals(document.getType())) {
                    jsonCommentRoot.element("title", commentRoot.getProperty(schemaPrefix, "title"));

                    // Attached files
                    jsonCommentRoot.element("files", buildFilesInfos(commentRoot));

                }

                jsonCommentRoot.element("children", getCommentsThread(commentRoot, commentableDoc, new JSONArray(), jsonConfig));
                commentsTree.add(jsonCommentRoot);

                if (StringUtils.isBlank(author)) {
                    log.warn("Missing comment author on comment ID '" + commentRoot.getId() + "' (content: '" + commentRoot.getProperty(schemaPrefix, "text")
                            + "')");
                }
            }
        }

        return createBlob(commentsTree);

    }

    private JSONArray getCommentsThread(DocumentModel comment, CommentableDocument commentableDocService, JSONArray threads, JsonConfig jsonConfig)
             {
        String schemaPrefix = getSchema(document.getType());
        List<DocumentModel> childrenComments = commentableDocService.getComments(comment);

        if (childrenComments == null || childrenComments.isEmpty()) {
            return threads;
        } else {
            for (DocumentModel childComment : childrenComments) {
                JSONObject jsonChildComment = new JSONObject();

                jsonChildComment.element("id", childComment.getId());
                jsonChildComment.element("path", childComment.getPathAsString());
                String author = (String) childComment.getProperty(schemaPrefix, "author");
                jsonChildComment.element("author", author);

                jsonChildComment.element("creationDate", childComment.getProperty(schemaPrefix, "creationDate"), jsonConfig);
                jsonChildComment.element("content", childComment.getProperty(schemaPrefix, "text"));
                jsonChildComment.element("modifiedDate", childComment.getProperty("dublincore", "modified"), jsonConfig);

                boolean canDelete = canDeleteComment(author, document);
                jsonChildComment.element("canDelete", canDelete);

                if (THREAD_TYPE.equals(document.getType())) {
                    jsonChildComment.element("title", childComment.getProperty(schemaPrefix, "title"));

                    // Attached files
                    jsonChildComment.element("files", buildFilesInfos(childComment));
                }
                jsonChildComment.element("children", getCommentsThread(childComment, commentableDocService, new JSONArray(), jsonConfig));
                threads.add(jsonChildComment);

                if (StringUtils.isBlank(author)) {
                    log.warn("Missing comment author on comment ID '" + childComment.getId() + "' (content: '" + childComment.getProperty(schemaPrefix, "text")
                            + "')");
                }
            }
            return threads;
        }
    }

    /**
     * @param childComment
     * @return
     */
    protected JSONArray buildFilesInfos(DocumentModel childComment) {
        // Attached files
        List<Map<String, Object>> attachedFiles = (List<Map<String, Object>>) childComment.getPropertyValue("files:files");

        JSONArray files = new JSONArray();
        int index = 0;

        for (Map<String, Object> attachedFile : attachedFiles) {
            // File infos
            JSONObject fileInfos = new JSONObject();
            //StorageBlob file = (StorageBlob) attachedFile.get("file");

            // TODO LBI tester ce code post migration nx10
            fileInfos.putAll(attachedFile);

/*            fileInfos.element("index", index);
            fileInfos.element("filename", file.getFilename());
            fileInfos.element("mime-type", file.getMimeType());
            fileInfos.element("length", file.getLength());

            files.add(fileInfos);*/

            index++;
        }
        return files;
    }


    private Blob createBlob(JSONArray json) {
        return new StringBlob(json.toString(), "application/json");
    }

    private boolean canDeleteComment(String author, DocumentModel document) {
        boolean canDelete = false;
        Principal user = session.getPrincipal();
        if (user != null) {
            boolean isUserAuthor = user.getName().equals(author);
            boolean isUserAdmin = ((NuxeoPrincipal) user).isAdministrator();
            boolean userHasAllRights = session.hasPermission(document.getRef(), SecurityConstants.EVERYTHING);
            canDelete = isUserAuthor || isUserAdmin || userHasAllRights;
        }
        return canDelete;
    }

    protected static String getSchema(String documentType) {
        String schemaPrefix = FetchCommentsOfDocument.COMMENT_SCHEMA;
        if (THREAD_TYPE.equals(documentType)) {
            schemaPrefix = FetchCommentsOfDocument.POST_SCHEMA;
        }
        return schemaPrefix;
    }

    private class GregorianCalendarJsonValueProcessor extends JsDateJsonValueProcessor {

        public GregorianCalendarJsonValueProcessor() {
            super();
        }

        @Override
        public Object processObjectValue(String s, Object o, JsonConfig jsonConfig) {
            Object po = o;

            if (o instanceof GregorianCalendar) {
                po = ((GregorianCalendar) o).getTime();
            }

            JSONObject processedValue = (JSONObject) super.processObjectValue(s, po, jsonConfig);
            if (!processedValue.isNullObject() && processedValue.containsKey("hours")) {
                processedValue.element("timeInMillis", ((GregorianCalendar) o).getTimeInMillis());
            }

            return processedValue;
        }

    }

}
