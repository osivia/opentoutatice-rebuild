/**
 * 
 */
package fr.toutatice.ecm.es.customizer.nx.listener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * This Listener is used to detect modifications on dc:title.
 * In case of title modification, the document context is marked.
 * 
 * @author Loïc Billon
 *
 */
public class TitleModificationListener implements EventListener {

    /**
	 * 
	 */
	public static final String DOC_CONTEXT_TITLE_MODIFICATION = "titleModification";
	
	private Log log = LogFactory.getLog(TitleModificationListener.class);

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nuxeo.ecm.core.event.EventListener#handleEvent(org.nuxeo.ecm.core.event.
	 * Event)
	 */
	@Override
	public void handleEvent(Event event) {
		DocumentEventContext context = (DocumentEventContext) event.getContext();
		DocumentModel sourceDocument = context.getSourceDocument();

		if (sourceDocument != null) {
			if (sourceDocument.hasFacet("Space") && isRootSpace(sourceDocument)) {

				boolean titleModification = true;
				List<String> lastDirtyFields = getDirtyPropertiesXPath(sourceDocument);
				for (String xpath : lastDirtyFields) {
					if(xpath.equals("dublincore:title")) {
						titleModification = true;
					}
				}


				if(titleModification) {
					
					context.getProperties().put(DOC_CONTEXT_TITLE_MODIFICATION, Boolean.TRUE);
					log.debug("Title has changed "+sourceDocument.getTitle());

				}

			}
		}

	}

	/**
	 * Get list of properties modified in the document before commit
	 * 
	 * @param doc
	 * @return
	 * @throws ClientException
	 */
	private List<String> getDirtyPropertiesXPath(DocumentModel doc) {
		List<String> dirtyPropertiesName = new ArrayList<String>();
		DocumentPart[] docParts = doc.getParts();
		for (DocumentPart docPart : docParts) {
			Iterator<Property> dirtyChildrenIterator = docPart.getDirtyChildren();
			while (dirtyChildrenIterator.hasNext()) {
				Property property = dirtyChildrenIterator.next();
				if (!property.isContainer() && property.isDirty()) {
					dirtyPropertiesName.add(docPart.getName() + ":" + property.getField().getName().getLocalName());
				} else {
					List<Property> dirtyProps = addChildrenDirtyProperties(property, new ArrayList<Property>());
					for (Property dirtyProperty : dirtyProps) {
						dirtyPropertiesName.add(docPart.getName() + ":" + dirtyProperty.getPath().substring(1));
					}
				}
			}
		}
		return dirtyPropertiesName;
	}

	/**
	 * 
	 * @param property
	 * @param dirtyProperties
	 * @return
	 */
	private List<Property> addChildrenDirtyProperties(Property property, List<Property> dirtyProperties) {
		if (!property.isContainer() && property.isDirty()) {
			dirtyProperties.add(property);
			return dirtyProperties;
		} else {
			Iterator<Property> dirtyChildrenIterator = property.getDirtyChildren();
			while (dirtyChildrenIterator.hasNext()) {
				Property chilProperty = dirtyChildrenIterator.next();
				dirtyProperties = addChildrenDirtyProperties(chilProperty, dirtyProperties);
			}
			return dirtyProperties;
		}
	}

	/**
	 * Test if document is a root space (behind the domain).
	 * 
	 * @param sourceDocument
	 * @return
	 */
	private boolean isRootSpace(DocumentModel sourceDocument) {

		CoreSession session = sourceDocument.getCoreSession();

		List<DocumentModel> parentDocuments = session.getParentDocuments(sourceDocument.getRef());

		DocumentModel rootSpace = null;
		for (DocumentModel element : parentDocuments) {
			if (element.hasFacet("Space") && !element.getType().equals("Domain")) {

				rootSpace = element;
				break;
			}
		}

		if (rootSpace.getId().equals(sourceDocument.getId())) {
			return true;
		} else
			return false;
	}

}
