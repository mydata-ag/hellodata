/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.sidecars.superset.service.user;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUserUpdateResponse;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import ch.bedag.dap.hellodata.sidecars.superset.service.resource.UserResourceProviderService;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserRolesUpdate;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_USER_CONTEXT_ROLE;
import static ch.bedag.dap.hellodata.sidecars.superset.service.user.RoleUtil.removeAllDashboardRoles;
import static ch.bedag.dap.hellodata.sidecars.superset.service.user.RoleUtil.removePublicRoleIfAdded;
import static ch.bedag.dap.hellodata.sidecars.superset.service.user.RoleUtil.removeRoleFromUser;

@Log4j2
@Service
@AllArgsConstructor
public class SupersetUpdateUserContextRoleConsumer {

    private static final String ADMIN_ROLE_NAME = "Admin";
    private static final String SQL_LAB_ROLE_NAME = "sql_lab";

    private final SupersetClientProvider supersetClientProvider;
    private final HelloDataContextConfig helloDataContextConfig;
    private final UserResourceProviderService userResourceProviderService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = UPDATE_USER_CONTEXT_ROLE)
    public CompletableFuture<Void> subscribe(UserContextRoleUpdate userContextRoleUpdate) throws URISyntaxException, IOException {
        log.info("-=-=-=-= RECEIVED USER CONTEXT ROLES UPDATE: payload: {}", userContextRoleUpdate);
        String dataDomainKey = helloDataContextConfig.getContext().getKey();
        SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
        Optional<SubsystemUser> supersetUserResult =
                supersetClient.users().getResult().stream().filter(user -> user.getEmail().equalsIgnoreCase(userContextRoleUpdate.getEmail())).findFirst();
        Optional<UserContextRoleUpdate.ContextRole> dataDomainContextRole =
                userContextRoleUpdate.getContextRoles().stream().filter(contextRole -> contextRole.getContextKey().equalsIgnoreCase(dataDomainKey)).findFirst();
        if (supersetUserResult.isPresent() && dataDomainContextRole.isPresent()) {
            SupersetRolesResponse allRoles = supersetClient.roles();
            UserContextRoleUpdate.ContextRole contextRole = dataDomainContextRole.get();
            SubsystemUser subsystemUser = supersetUserResult.get();
            List<SupersetRole> supersetUserRoles = subsystemUser.getRoles();
            SupersetUserRolesUpdate supersetUserRolesUpdate = new SupersetUserRolesUpdate();
            supersetUserRolesUpdate.setRoles(subsystemUser.getRoles().stream().map(SupersetRole::getId).toList());
            removeBiRoles(allRoles, supersetUserRolesUpdate);
            assignAdminRoleIfSuperuser(userContextRoleUpdate, allRoles, supersetUserRolesUpdate);
            switch (contextRole.getRoleName()) {
                case DATA_DOMAIN_ADMIN -> {
                    removeAllDashboardRoles(allRoles, supersetUserRolesUpdate);
                    assignRoleToUser(SlugifyUtil.BI_ADMIN_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                    assignRoleToUser(SlugifyUtil.BI_EDITOR_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                    assignRoleToUser(SQL_LAB_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                }
                case DATA_DOMAIN_EDITOR -> {
                    removeAllDashboardRoles(allRoles, supersetUserRolesUpdate);
                    assignRoleToUser(SlugifyUtil.BI_EDITOR_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                    assignRoleToUser(SQL_LAB_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                }
                case DATA_DOMAIN_VIEWER -> assignRoleToUser(SlugifyUtil.BI_VIEWER_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                case NONE -> {
                    removeAllDashboardRoles(allRoles, supersetUserRolesUpdate);
                    assignRoleToUser(SlugifyUtil.BI_VIEWER_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                }
                default -> log.debug("Irrelevant role name? {}", contextRole.getRoleName());
            }
            removePublicRoleIfAdded(allRoles, supersetUserRolesUpdate);
            SupersetUserUpdateResponse updatedUser = supersetClientProvider.getSupersetClientInstance().updateUserRoles(supersetUserRolesUpdate, subsystemUser.getId());
            log.info("-=-=-=-= UPDATED USER ROLES: user: {}, role ids: {}", userContextRoleUpdate.getEmail(), updatedUser.getResult().getRoles());
            userResourceProviderService.publishUsers();
        }
        return null;
    }

    private void removeBiRoles(SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        removeRoleFromUser(SlugifyUtil.BI_ADMIN_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        removeRoleFromUser(SlugifyUtil.BI_VIEWER_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        removeRoleFromUser(SlugifyUtil.BI_EDITOR_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        removeRoleFromUser(SQL_LAB_ROLE_NAME, allRoles, supersetUserRolesUpdate);
    }

    private void assignAdminRoleIfSuperuser(UserContextRoleUpdate userContextRoleUpdate, SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        if (userContextRoleUpdate.getContextRoles().stream().anyMatch(ctxRole -> ctxRole.getRoleName() == HdRoleName.HELLODATA_ADMIN)) {
            assignRoleToUser(ADMIN_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        } else {
            removeRoleFromUser(ADMIN_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        }
    }

    private void assignRoleToUser(String roleName, SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        List<Integer> roles = allRoles.getResult().stream().filter(role -> role.getName().equalsIgnoreCase(roleName)).map(SupersetRole::getId).toList();
        List<Integer> userRoles = supersetUserRolesUpdate.getRoles();
        supersetUserRolesUpdate.setRoles(Stream.concat(roles.stream(), userRoles.stream()).toList());
    }
}
