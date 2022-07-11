///*
// * (C) Copyright 2014 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
// *
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the GNU Lesser General Public License
// * (LGPL) version 2.1 which accompanies this distribution, and is available at
// * http://www.gnu.org/licenses/lgpl-2.1.html
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * Lesser General Public License for more details.
// *
// *
// * Contributors:
// *   mberhaut1
// *    
// */
//package fr.toutatice.ecm.platform.core.components;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.commons.lang.StringUtils;
//import org.nuxeo.ecm.core.storage.sql.coremodel.SQLDocumentLive;
//import org.nuxeo.ecm.core.versioning.VersioningService;
//
//public class ToutaticeVersioningServiceHandler<T> extends ToutaticeAbstractServiceHandler<T> {
//
//	private static final List<String> filteredMethodsList = new ArrayList<String>() {
//		private static final long serialVersionUID = 1L;
//
//		{
//			add("doPostCreate");
//			add("doPreSave");
//			add("doPostSave");
//			add("doCheckIn");
//			add("doCheckOut");
//		}
//	};
//	
//	@Override
//	public T newProxy(T object, Class<T> itf) {
//		setObject(object);
//		return itf.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { itf }, this));
//	}
//
//	@Override
//	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//	    String sessionId = null;
//	    
//		try {
//			if (filteredMethodsList.contains(method.getName())) {
//				if (null != args && 0 < args.length) {
//					for (Object arg : args) {
//						if (arg instanceof SQLDocumentLive) {
//							SQLDocumentLive document = (SQLDocumentLive) args[0];
//							sessionId = document.getSession().getSessionId();
//							break;
//						}
//					}
//					
//					if (StringUtils.isNotBlank(sessionId) && ToutaticeServiceProvider.instance().isRegistered(VersioningService.class, sessionId)) {
//						// do filter invocation
//						return null;
//					}
//				}
//			}
//			
//			return method.invoke(this.object, args);
//		} catch (InvocationTargetException e) {
//			throw e.getCause();
//		}
//	}
//	
//}
