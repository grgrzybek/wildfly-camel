/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


package org.wildfly.extension.camel.deployment;

import java.io.IOException;
import java.net.URL;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.gravia.repository.DefaultRepositoryXMLReader;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.repository.RepositoryReader;
import org.jboss.gravia.repository.RepositoryStorage;
import org.jboss.gravia.resource.Resource;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.gravia.GraviaConstants;

/**
 * Processes repository content deployments.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Jun-2013
 */
public class RepositoryContentInstallProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final String runtimeName = depUnit.getName();

        URL contentURL = null;
        try {
            if (runtimeName.endsWith(CamelConstants.REPOSITORY_CONTENT_FILE_SUFFIX)) {
                contentURL = depUnit.getAttachment(Attachments.DEPLOYMENT_CONTENTS).asFileURL();
            } else {
                VirtualFile rootFile = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
                VirtualFile child = rootFile.getChild(CamelConstants.REPOSITORY_CONTENT_FILE_NAME);
                contentURL = child.isFile() ? child.asFileURL() : null;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create camel context: " + runtimeName, ex); 
        }

        if (contentURL != null) {
            Repository repository = depUnit.getAttachment(GraviaConstants.REPOSITORY_KEY);
            RepositoryReader reader;
            try {
                reader = new DefaultRepositoryXMLReader(contentURL.openStream());
            } catch (IOException ex) {
                throw new DeploymentUnitProcessingException(ex);
            }
            Resource res = reader.nextResource();
            while (res != null) {
                RepositoryStorage storage = repository.adapt(RepositoryStorage.class);
                if (storage.getResource(res.getIdentity()) == null) {
                    storage.addResource(res);
                }
                res = reader.nextResource();
            }
        }

    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
