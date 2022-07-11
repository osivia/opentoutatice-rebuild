/**
 * 
 */

package fr.toutatice.ecm.es.customizer.registry;

import java.util.LinkedList;
import java.util.List;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import fr.toutatice.ecm.es.customizer.listeners.api.ICustomESListener;
import fr.toutatice.ecm.es.customizer.writers.api.ICustomJsonESWriter;


/**
 * @author david
 */
public class ESCustomizersServiceRegistry extends DefaultComponent {
	
	/**
	 * Custom ES writer point.
	 */
	private static String WRITERS_EXT_POINT = "writers";
	
	/**
	 * Custom ES listener point.
	 */
	private static String LISTENERS_EXT_POINT = "listeners";
	
	/**
	 * Custom writers.
	 */
	private List<ICustomJsonESWriter> writers;
	
	/**
	 * Custom listeners.
	 */
	private List<ICustomESListener> listeners;
	
	/**
	 * @return registered custom ES writers.
	 */
	public List<ICustomJsonESWriter> getCustomJsonESWriters(){
		return this.writers;
	}
	
    /**
     * @return the listeners
     */
    public List<ICustomESListener> getCustomESListeners() {
        return listeners;
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        writers = new LinkedList<ICustomJsonESWriter>();
        listeners = new LinkedList<ICustomESListener>();
    }
	
	@Override
	public void registerContribution(Object contribution,
			String extensionPoint, ComponentInstance contributor) {
		
		if(WRITERS_EXT_POINT.equals(extensionPoint)){
			JsonESWriterDescriptor desc = (JsonESWriterDescriptor) contribution;
			if(desc.isEnabled()){
				String className = desc.getClazz();
				ICustomJsonESWriter clazzInstance;
				try {
					clazzInstance = (ICustomJsonESWriter) Class.forName(className).newInstance();
					int order = desc.getOrder();
					writers.add(order, clazzInstance);
					
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} else if(LISTENERS_EXT_POINT.equals(extensionPoint)) {
		    ESListenerDescriptor desc = (ESListenerDescriptor) contribution;
		    if(desc.isEnabled()){
		        String className = desc.getClazz();
		        ICustomESListener clazzInstance;
				try {
					clazzInstance = (ICustomESListener) Class.forName(className).newInstance();
			        int order = desc.getOrder();
			        listeners.add(order, clazzInstance);
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

		    }
		}
		
	}

}
