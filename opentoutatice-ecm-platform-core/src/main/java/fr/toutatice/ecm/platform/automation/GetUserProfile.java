/*
 * (C) Copyright 2014 Académie de Rennes (http://www.ac-rennes.fr/) and others.
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
package fr.toutatice.ecm.platform.automation;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.user.center.profile.UserProfileService;

@Operation(id = GetUserProfile.ID, category = Constants.CAT_SERVICES, label = "Get UserProfile", since = "5.6.0",
        description = "Get the user profile document of the connected user (may create it and the user worksapce if one doesn't exist")
public class GetUserProfile {

    public static final String ID = "Services.GetToutaticeUserProfile";

    private static final Log log = LogFactory.getLog(GetUserProfile.class);

    @Param(name = "username", required = false)
    protected String username;

    @Context
    protected OperationContext ctx;

    @Context
    protected UserProfileService userProfileService;

    @OperationMethod
    public Object run() {
        DocumentModel userProfile = null;
        NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
        try {
            if (StringUtils.isNotBlank(username)) {
                userProfile = userProfileService.getUserProfileDocument(username, ctx.getCoreSession());
            } else {
                userProfile = userProfileService.getUserProfileDocument(ctx.getCoreSession());
            }
        } catch (Exception e) {
            log.error("Failed to get the user profil document for user '" + principal.getName() + "', error: " + e.getMessage());
            //throw new ClientException("Failed to get the user profil document");
        }

        return userProfile;
    }

}
