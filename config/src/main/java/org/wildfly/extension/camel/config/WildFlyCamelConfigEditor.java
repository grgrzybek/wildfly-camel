/*
 * Copyright 2015 JBoss Inc
 *
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
 */
package org.wildfly.extension.camel.config;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;


public final class WildFlyCamelConfigEditor implements ConfigEditor {

    public static final Namespace NS_DOMAIN = Namespace.getNamespace("urn:jboss:domain:1.7");
    public static final Namespace NS_CAMEL = Namespace.getNamespace("urn:jboss:domain:camel:1.0");
    public static final Namespace NS_SECURITY = Namespace.getNamespace("urn:jboss:domain:security:1.2");
    public static final Namespace NS_WELD = Namespace.getNamespace("urn:jboss:domain:weld:1.0");

    @Override
    public void applyStandaloneConfigChange(boolean enable, Document doc) {
        updateExtension(enable, doc);
        updateSubsystem(enable, doc);
        // updateWeldConfig(enable, doc);
        updateHawtIOSystemProperties(enable, doc);
        updateHawtIOSecurityDomain(enable, doc);
    }

    @Override
    public void applyDomainConfigChange(boolean enable, Document doc) {
        applyStandaloneConfigChange(enable, doc);
        updateServergroup(enable, doc);
    }

    private static void updateExtension(boolean enable, Document doc) {
        Element extensions = doc.getRootElement().getChild("extensions", NS_DOMAIN);
        ConfigSupport.assertExists(extensions, "Did not find the <extensions> element");
        Element element = ConfigSupport.findElementWithAttributeValue(extensions, "extension", NS_DOMAIN, "module", "org.wildfly.extension.camel");
        if (enable && element == null) {
            extensions.addContent(new Text("    "));
            extensions.addContent(new Element("extension", NS_DOMAIN).setAttribute("module", "org.wildfly.extension.camel"));
            extensions.addContent(new Text("\n    "));
        }
        if (!enable && element != null) {
            element.getParentElement().removeContent(element);
        }
    }

    private static void updateWeldConfig(boolean enable, Document doc) {
        List<Element> profiles = ConfigSupport.findProfileElements(doc, NS_DOMAIN);
        for (Element profile : profiles) {
            Element weld = profile.getChild("subsystem", NS_WELD);
            ConfigSupport.assertExists(weld, "Did not find the weld subsystem");
            if (enable) {
                weld.setAttribute("require-bean-descriptor", "true");
            } else {
                weld.removeAttribute("require-bean-descriptor");
            }
        }
    }

    private static void updateSubsystem(boolean enable, Document doc) {
        List<Element> profiles = ConfigSupport.findProfileElements(doc, NS_DOMAIN);
        for (Element profile : profiles) {
            Element element = profile.getChild("subsystem", NS_CAMEL);
            if (enable && element == null) {
                URL resource = WildFlyCamelConfigEditor.class.getResource("camel-subsystem.xml");
                profile.addContent(new Text("    "));
                profile.addContent(ConfigSupport.loadElementFrom(resource));
                profile.addContent(new Text("\n    "));
            }
            if (!enable && element != null) {
                element.getParentElement().removeContent(element);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void updateHawtIOSystemProperties(boolean enable, Document doc) {
        Element rootElement = doc.getRootElement();
        Element element = rootElement.getChild("system-properties", NS_DOMAIN);
        if (element == null) {
            element = new Element("system-properties", NS_DOMAIN);
            element.addContent(new Text("\n    "));

            int pos = rootElement.indexOf(rootElement.getChild("extensions", NS_DOMAIN));
            rootElement.addContent(pos + 1, new Text("    "));
            rootElement.addContent(pos + 1, element);
            rootElement.addContent(pos + 1, new Text("\n"));
        }

        LinkedHashMap<String, Element> propertiesByName = ConfigSupport.mapByAttributeName(element.getChildren(), "name");
        if (enable) {
            addProperty(element, propertiesByName, "hawtio.authenticationEnabled", "true");
            addProperty(element, propertiesByName, "hawtio.offline", "true");
            addProperty(element, propertiesByName, "hawtio.realm", "hawtio-domain");
        } else {
            rmProperty(propertiesByName, "hawtio.authenticationEnabled");
            rmProperty(propertiesByName, "hawtio.offline");
            rmProperty(propertiesByName, "hawtio.realm");
        }
    }

    private static void updateServergroup(boolean enable, Document doc) {
        Element serverGroups = doc.getRootElement().getChild("server-groups", NS_DOMAIN);
        Element camel = ConfigSupport.findElementWithAttributeValue(serverGroups, "server-group", NS_DOMAIN, "name", "camel-server-group");
        if (enable && camel == null) {
            URL resource = WildFlyCamelConfigEditor.class.getResource("camel-servergroup.xml");
            serverGroups.addContent(new Text("    "));
            serverGroups.addContent(ConfigSupport.loadElementFrom(resource));
            serverGroups.addContent(new Text("\n    "));
        }
        if (!enable && camel != null) {
            camel.getParentElement().removeContent(camel);
        }
    }

    private static void addProperty(Element systemProperties, LinkedHashMap<String, Element> propertiesByName, String name, String value) {
        if (!propertiesByName.containsKey(name)) {
            systemProperties.addContent(new Text("   "));
            systemProperties.addContent(new Element("property", NS_DOMAIN).setAttribute("name", name).setAttribute("value", value));
            systemProperties.addContent(new Text("\n    "));
        }
    }

    private static void rmProperty(LinkedHashMap<String, Element> propertiesByName, String name) {
        Element element = propertiesByName.get(name);
        if (element != null) {
            element.getParentElement().removeContent(element);
        }
    }

    private static void updateHawtIOSecurityDomain(boolean enable, Document doc) {
        List<Element> profiles = ConfigSupport.findProfileElements(doc, NS_DOMAIN);
        for (Element profile : profiles) {
            Element security = profile.getChild("subsystem", NS_SECURITY);
            ConfigSupport.assertExists(security, "Did not find the security subsystem");
            Element domains = security.getChild("security-domains", NS_SECURITY);
            ConfigSupport.assertExists(domains, "Did not find the <security-domains> element");
            Element hawtioDomain = ConfigSupport.findElementWithAttributeValue(domains, "security-domain", NS_SECURITY, "name", "hawtio-domain");
            if (enable && hawtioDomain == null) {
                URL resource = WildFlyCamelConfigEditor.class.getResource("hawtio-security-domain.xml");
                domains.addContent(new Text("    "));
                domains.addContent(ConfigSupport.loadElementFrom(resource));
                domains.addContent(new Text("\n            "));
            }
            if (!enable && hawtioDomain != null) {
                hawtioDomain.getParentElement().removeContent(hawtioDomain);
            }
        }
    }
}
