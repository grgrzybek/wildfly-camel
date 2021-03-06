/*
 * #%L
 * Wildfly Camel :: Testsuite
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


package org.wildfly.camel.test.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.ResourceHandle;
import org.jboss.gravia.repository.DefaultRepositoryXMLReader;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.repository.RepositoryReader;
import org.jboss.gravia.repository.RepositoryStorage;
import org.jboss.gravia.resource.DefaultRequirementBuilder;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 10-May-2013
 */
public class ProvisionerSupport {

    private Provisioner provisioner;

    public ProvisionerSupport(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    public Provisioner getResourceProvisioner() {
        return provisioner;
    }

    public Repository getRepository() {
        return provisioner.getRepository();
    }

    public List<ResourceHandle> installCapabilities(String namespace, String... features) throws Exception {
        IllegalArgumentAssertion.assertNotNull(namespace, "namespace");
        IllegalArgumentAssertion.assertNotNull(features, "features");

        Requirement[] reqs = new Requirement[features.length];
        for (int i=0; i < features.length; i++) {
            reqs[i] = new DefaultRequirementBuilder(namespace, features[i]).getRequirement();
        }
        return installCapabilities(reqs);
    }

    public List<ResourceHandle> installCapabilities(Requirement... reqs) throws Exception {
        IllegalArgumentAssertion.assertNotNull(reqs, "reqs");
        
        Set<Requirement> reqset = new HashSet<>(Arrays.asList(reqs));
        Set<ResourceHandle> reshandles = provisioner.provisionResources(reqset);
        
        return Collections.unmodifiableList(new ArrayList<>(reshandles));
    }

    public void populateRepository(ClassLoader classLoader, String... features) throws IOException {
        IllegalArgumentAssertion.assertNotNull(features, "features");

        for (String feature : features) {
            InputStream input = getFeatureResource(classLoader, feature);
            if (input != null) {
                RepositoryReader reader = new DefaultRepositoryXMLReader(input);
                Resource auxres = reader.nextResource();
                while (auxres != null) {
                    RepositoryStorage storage = getRepository().adapt(RepositoryStorage.class);
                    if (storage.getResource(auxres.getIdentity()) == null) {
                        storage.addResource(auxres);
                    }
                    auxres = reader.nextResource();
                }
            }
        }
    }

    private InputStream getFeatureResource(ClassLoader classLoader, String feature) {
        return classLoader.getResourceAsStream("/repository/" + feature  + ".xml");
    }
}
