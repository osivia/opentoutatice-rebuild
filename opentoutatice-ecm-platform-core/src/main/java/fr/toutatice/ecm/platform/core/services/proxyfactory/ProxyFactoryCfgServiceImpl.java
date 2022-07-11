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
package fr.toutatice.ecm.platform.core.services.proxyfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

import fr.toutatice.ecm.platform.core.components.ToutaticeServiceProvider;

public class ProxyFactoryCfgServiceImpl<T> extends DefaultComponent implements ProxyFactoryCfgService<T> {

	private static final long serialVersionUID = 1062195510415601168L;

	public static final ComponentName ID = new ComponentName("fr.toutatice.ecm.platform.service.proxyfactory.ProxyFactoryCfgService");

	private static final Log log = LogFactory.getLog(ProxyFactoryCfgServiceImpl.class);
	public static final String EXTENSION_POINT = "handlers";

	protected final Map<String, ProxyFactoryCfgDescriptor> descriptors;

	public ProxyFactoryCfgServiceImpl() {
		this.descriptors = new HashMap<String, ProxyFactoryCfgDescriptor>();
	}

	@Override
	public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
		if (EXTENSION_POINT.equals(extensionPoint)) {
			ProxyFactoryCfgDescriptor desc = (ProxyFactoryCfgDescriptor) contribution;
			if (desc.isEnabled()) {
				this.descriptors.put(desc.getServiceClass(), desc);
				log.info((new StringBuilder()).append(" Added descriptor ").append(desc.getServiceClass()).toString());
			}
		}
	}

	@Override
	public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
		if (EXTENSION_POINT.equals(extensionPoint)) {
			ProxyFactoryCfgDescriptor desc = (ProxyFactoryCfgDescriptor) contribution;
			this.descriptors.remove(desc.getServiceClass());
			log.info((new StringBuilder()).append(" Removed descriptor ").append(desc.getServiceClass()).toString());
		}
	}
	
	@Override
	public void activate(ComponentContext context) {
		super.activate(context);
		
		// Install the service provider
		ToutaticeServiceProvider.instance().install();
	}
	
	@Override
	public void deactivate(ComponentContext context) {
		super.deactivate(context);
		descriptors.clear();
		
		// Uninstall the service provider
		ToutaticeServiceProvider.instance().uninstall();
	}
	
	@Override
	public Class<?> getServiceHandler(Class<T> clazz) throws ClassNotFoundException {
		Class<?> t = null;
		String handlerClassName = clazz.getName();
		
		// look among contributions whether a service proxy is configured (and enabled)
		if (this.descriptors.containsKey(clazz.getName())) {
			ProxyFactoryCfgDescriptor handlerDescriptor = this.descriptors.get(clazz.getName());
			handlerClassName = handlerDescriptor.getHandlerClass();
			t = Class.forName(handlerClassName);
		}
		
		return t;
	}

	@Override
	public List<Class<?>> getAllServicesHandlers() throws ClassNotFoundException {
		List<Class<?>> handlers = new ArrayList<Class<?>>();
		
		for (String serviceClassName : this.descriptors.keySet()) {
			handlers.add(Class.forName(serviceClassName));
		}
		
		return handlers;
	}

}
