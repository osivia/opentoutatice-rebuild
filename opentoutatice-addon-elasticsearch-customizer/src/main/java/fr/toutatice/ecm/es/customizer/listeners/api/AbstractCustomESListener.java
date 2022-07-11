/**
 * 
 */
package fr.toutatice.ecm.es.customizer.listeners.api;

import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import fr.toutatice.ecm.es.customizer.nx.listener.ESInlineListenerCustomizer;


/**
 * @author david
 *
 */
public abstract class AbstractCustomESListener implements ICustomESListener {

    /** Referenced listener enabling customizing. */
    private ESInlineListenerCustomizer esListener;

    /**
     * Default constructor.
     */
    public AbstractCustomESListener() {
        super();
    }

    /**
     * Constructor.
     */
    public AbstractCustomESListener(ESInlineListenerCustomizer esListener) {
        super();
        this.esListener = esListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setESInlineListener(ESInlineListenerCustomizer esListener) {
        this.esListener = esListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void customStackCommands(DocumentEventContext docCtx, String eventId);

}
