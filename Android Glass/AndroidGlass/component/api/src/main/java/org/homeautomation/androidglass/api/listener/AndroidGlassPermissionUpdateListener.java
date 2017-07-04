/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.homeautomation.androidglass.api.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.homeautomation.androidglass.api.constants.AndroidGlassConstants;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AndroidGlassPermissionUpdateListener implements ServletContextListener {

    private static Log log = LogFactory.getLog(AndroidGlassPermissionUpdateListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        UserStoreManager userStoreManager = getUserStoreManager();
        try {
            if (userStoreManager != null) {
                if (!userStoreManager.isExistingRole(AndroidGlassConstants.ROLE_NAME)) {
                    userStoreManager.addRole(AndroidGlassConstants.ROLE_NAME, null, getPermissions());
                } else {
                    getAuthorizationManager().authorizeRole(AndroidGlassConstants.ROLE_NAME,
                            AndroidGlassConstants.PERM_ENROLL_PICAVI, CarbonConstants.UI_PERMISSION_ACTION);
                    getAuthorizationManager().authorizeRole(AndroidGlassConstants.ROLE_NAME,
                            AndroidGlassConstants.PERM_OWNING_DEVICE_VIEW, CarbonConstants.UI_PERMISSION_ACTION);
                }
            } } catch (UserStoreException e) {
            log.error("Error while creating a role and adding a user for Android Sense.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }

    public static UserStoreManager getUserStoreManager() {
        RealmService realmService;
        UserStoreManager userStoreManager;
        try {
            PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            realmService = (RealmService) ctx.getOSGiService(RealmService.class, null);
            if (realmService == null) {
                String msg = "Realm service has not initialized.";
                log.error(msg);
                throw new IllegalStateException(msg);
            }
            int tenantId = ctx.getTenantId();
            userStoreManager = realmService.getTenantUserRealm(tenantId).getUserStoreManager();
            realmService.getTenantUserRealm(tenantId).getAuthorizationManager();
        } catch (UserStoreException e) {
            String msg = "Error occurred while retrieving current user store manager";
            log.error(msg, e);
            throw new IllegalStateException(msg);
        }
        return userStoreManager;
    }

    public static AuthorizationManager getAuthorizationManager() {
        RealmService realmService;
        AuthorizationManager authorizationManager;
        try {
            PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            realmService = (RealmService) ctx.getOSGiService(RealmService.class, null);
            if (realmService == null) {
                String msg = "Realm service has not initialized.";
                log.error(msg);
                throw new IllegalStateException(msg);
            }
            int tenantId = ctx.getTenantId();
            authorizationManager = realmService.getTenantUserRealm(tenantId).getAuthorizationManager();
        } catch (UserStoreException e) {
            String msg = "Error occurred while retrieving current user store manager";
            log.error(msg, e);
            throw new IllegalStateException(msg);
        }
        return authorizationManager;
    }

    private Permission[] getPermissions() {

            Permission androidTv = new Permission(AndroidGlassConstants.PERM_ENROLL_PICAVI,
                    CarbonConstants.UI_PERMISSION_ACTION);
            Permission view = new Permission(AndroidGlassConstants.PERM_OWNING_DEVICE_VIEW, CarbonConstants
                    .UI_PERMISSION_ACTION);

            return new Permission[]{androidTv, view};
    }

}
