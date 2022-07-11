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
package fr.toutatice.ecm.platform.core.components;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public abstract class ToutaticeAbstractServiceHandler<T> implements InvocationHandler {
	protected T object;

	protected ToutaticeAbstractServiceHandler() {
		this.object = null;
	}
	
	protected ToutaticeAbstractServiceHandler(T object) {
		this();
		this.object = object;
	}
	
	protected void setObject(T object) {
		this.object = object;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return method.invoke(object, args);
	}
	
	public abstract T newProxy(T object, Class<T> itf);
}
