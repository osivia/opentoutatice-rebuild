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

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.platform.core.components.ToutaticeServiceProvider;

public abstract class ToutaticeSilentProcessRunnerHelper extends UnrestrictedSessionRunner {
	
	private static final Log log = LogFactory.getLog(ToutaticeSilentProcessRunnerHelper.class);
	
	public static final List<Class<?>> DEFAULT_FILTERED_SERVICES_LIST = new ArrayList<Class<?>>() {
		private static final long serialVersionUID = 1L;

		{
			add(EventService.class);
		}
	};
	
	public ToutaticeSilentProcessRunnerHelper(CoreSession session) {
		super(session);
	}

	public ToutaticeSilentProcessRunnerHelper(String repositoryName) {
		super(repositoryName);
	}

    protected ToutaticeSilentProcessRunnerHelper(String repositoryName, String originatingUser) {
    	super(repositoryName, originatingUser);
    }

    /**
     * Calls the {@link #run()} method with a silent mode. The dublincore events are disabled either for the current user session (usual run method is called)
     * or the for the system/administrator user session (unrestricted method is called).
     * 
     * @param runInUnrestrictedMode Indique si les traitements doivent être réalisés en mode unrestricted (avec l'utilisateur system/administrateur) 
     * @throws LoginException 
     * @throws ClientException
     */
    public void silentRun(boolean runInUnrestrictedMode) throws LoginException  {
    	silentRun(runInUnrestrictedMode, DEFAULT_FILTERED_SERVICES_LIST);
    }
    
    /**
     * Calls the {@link #run()} method with a silent mode. The dublincore events are disabled either for the current user session (usual run method is called)
     * or the for the system/administrator user session (unrestricted method is called).
     * 
     * @param runInUnrestrictedMode Indique si les traitements doivent être réalisés en mode unrestricted (avec l'utilisateur system/administrateur)
     * @param filteredServices the class name of services to filter (provided these one have a handler contributed onto the proxy factory service)
     * @throws LoginException 
     * @throws ClientException
     */
    public void silentRun(boolean runInUnrestrictedMode, List<Class<?>> filteredServices) throws LoginException { 

        log.debug("Démarrage de l'exécution d'un processus en mode silencieux");
        
        if (runInUnrestrictedMode) {
            runUnrestricted(filteredServices);
        } else {
            run(filteredServices);
        }

        log.debug("Fin de l'exécution d'un processus en mode silencieux");
    }
    
    private void runUnrestricted(List<Class<?>> filteredServices) throws LoginException {
        isUnrestricted = true;
        try {
            if (sessionIsAlreadyUnrestricted) {
                run(filteredServices);
                return;
            }

            LoginContext loginContext;
//            try {
                loginContext = Framework.loginAs(originatingUsername);
//            } catch (LoginException e) {
//                throw new Exception(e);
//            }
            
            try {
                CoreSession baseSession = session;
                if (baseSession != null
                        && !baseSession.isStateSharedByAllThreadSessions()) {
                    // save base session state for unrestricted one
                    baseSession.save();
                }

                session = CoreInstance.openCoreSession(repositoryName);
                if (loginContext == null && Framework.isTestModeSet()) {
                    NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
                    if (principal instanceof SystemPrincipal) {
                        // we are in a test that is not using authentication
                        // =>
                        // we're not stacking the originating user in the
                        // authentication stack
                        // so we're setting manually now
                        principal.setOriginatingUser(originatingUsername);
                    }
                }

                try {
                    run(filteredServices);
                } finally {
                    try {
                        if (!session.isStateSharedByAllThreadSessions()) {
                            // save unrestricted state for base session
                            session.save();
                        }
                        session.close();
                    } finally {
                        if (baseSession != null
                                && !baseSession.isStateSharedByAllThreadSessions()) {
                            // process invalidations from unrestricted session
                            baseSession.save();
                        }
                        session = baseSession;
                    }
                }
            } finally {

                // loginContext may be null in tests
                if (loginContext != null) {
                    loginContext.logout();
                }

            }
        } finally {
            isUnrestricted = false;
            if (Framework.isTestModeSet() && sessionIsAlreadyUnrestricted) {
                session.save();
            }
        }
    }

    private void run(List<Class<?>> filteredServices)  {
        String sessionId = session.getSessionId();
        try {
            // installer le service de filtrage pour l'utilisateur
            if (filteredServices != null) {
                for (Class<?> service : filteredServices) {
                    ToutaticeServiceProvider.instance().register(service, sessionId);
                }
            } else {
                ToutaticeServiceProvider.instance().registerAll(sessionId);
            }
            
            run();
        } finally {
            // désinstaller le service de filtrage pour l'utilisateur
            
            if (null != filteredServices) {
                for (Class<?> service : filteredServices) {
                    ToutaticeServiceProvider.instance().unregister(service, sessionId);
                }
            } else {
                ToutaticeServiceProvider.instance().unregisterAll(sessionId);
            }
            
            log.debug("Fin de l'exécution d'un processus en mode silencieux");
        }
    }
    
}
