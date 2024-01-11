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
package ch.bedag.dap.hellodata.sidecars.superset.service.resource;

import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.permission.PermissionResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.permission.response.superset.SupersetPermissionResponse;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import ch.bedag.dap.hellodata.sidecars.superset.service.cloud.PodUtilsProvider;
import io.kubernetes.client.openapi.models.V1Pod;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_PERMISSION_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class PermissionResourceProviderService {
    private final NatsSenderService natsSenderService;
    private final SupersetClientProvider supersetClientProvider;
    private final PodUtilsProvider podUtilsProvider;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Scheduled(fixedDelayString = "${hello-data.sidecar.publish-interval-seconds:300}", timeUnit = TimeUnit.SECONDS)
    public void publishPermissions() throws URISyntaxException, IOException {
        log.info("--> publishPermissions()");
        SupersetPermissionResponse response = supersetClientProvider.getSupersetClientInstance().permissions();

        PodUtils<V1Pod> podUtils = podUtilsProvider.getIfAvailable();
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            PermissionResource permissionResource = new PermissionResource(ModuleType.SUPERSET, this.instanceName, current.getMetadata().getNamespace(), response.getResult());
            natsSenderService.publishMessageToJetStream(PUBLISH_PERMISSION_RESOURCES, permissionResource);
        } else {
            //dummy info for tests
            PermissionResource permissionResource = new PermissionResource(ModuleType.SUPERSET, this.instanceName, "local", response.getResult());
            natsSenderService.publishMessageToJetStream(PUBLISH_PERMISSION_RESOURCES, permissionResource);
        }
    }
}
