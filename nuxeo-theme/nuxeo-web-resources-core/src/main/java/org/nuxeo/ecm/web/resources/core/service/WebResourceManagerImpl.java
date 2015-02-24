/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.nuxeo.ecm.web.resources.api.Processor;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.ResourceContext;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.core.ProcessorDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.2
 */
public class WebResourceManagerImpl extends DefaultComponent implements WebResourceManager {

    private static final Log log = LogFactory.getLog(WebResourceManagerImpl.class);

    protected static final String RESOURCES_ENDPOINT = "resources";

    protected ResourceRegistry resources;

    protected static final String RESOURCE_BUNDLES_ENDPOINT = "resourceBundles";

    protected ResourceBundleRegistry resourceBundles;

    protected static final String PROCESSORS_ENDPOINT = "processors";

    protected ProcessorRegistry processors;

    // Runtime Component API

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        resources = new ResourceRegistry();
        resourceBundles = new ResourceBundleRegistry();
        processors = new ProcessorRegistry();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (RESOURCES_ENDPOINT.equals(extensionPoint)) {
            Resource resource = (Resource) contribution;
            log.info(String.format("Register resource '%s'", resource.getName()));
            resources.addContribution(resource);
            log.info(String.format("Done registering resource '%s'", resource.getName()));
        } else if (RESOURCE_BUNDLES_ENDPOINT.equals(extensionPoint)) {
            ResourceBundle bundle = (ResourceBundle) contribution;
            log.info(String.format("Register resource bundle '%s'", bundle.getName()));
            resourceBundles.addContribution(bundle);
            log.info(String.format("Done registering resource bundle '%s'", bundle.getName()));
        } else if (PROCESSORS_ENDPOINT.equals(extensionPoint)) {
            ProcessorDescriptor p = (ProcessorDescriptor) contribution;
            log.info(String.format("Register processor '%s'", p.getName()));
            processors.addContribution(p);
            log.info(String.format("Done registering processor '%s'", p.getName()));
        } else {
            log.error(String.format("Unknown contribution to the service, extension point '%s': '%s", extensionPoint,
                    contribution));
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (RESOURCES_ENDPOINT.equals(extensionPoint)) {
            Resource resource = (Resource) contribution;
            log.info(String.format("Removing resource '%s'", resource.getName()));
            resources.removeContribution(resource);
            log.info(String.format("Done removing resource '%s'", resource.getName()));
        } else if (RESOURCE_BUNDLES_ENDPOINT.equals(extensionPoint)) {
            ResourceBundle resourceBundle = (ResourceBundle) contribution;
            log.info(String.format("Removing resource bundle '%s'", resourceBundle.getName()));
            resourceBundles.removeContribution(resourceBundle);
            log.info(String.format("Done removing resource bundle '%s'", resourceBundle.getName()));
        } else if (PROCESSORS_ENDPOINT.equals(extensionPoint)) {
            ProcessorDescriptor p = (ProcessorDescriptor) contribution;
            log.info(String.format("Removing processor '%s'", p.getName()));
            processors.removeContribution(p);
            log.info(String.format("Done removing processor '%s'", p.getName()));
        } else {
            log.error(String.format(
                    "Unknown contribution to the theme " + "styling service, extension point '%s': '%s",
                    extensionPoint, contribution));
        }
    }

    // service API

    @Override
    public Resource getResource(String name) {
        return resources.getResource(name);
    }

    @Override
    public ResourceBundle getResourceBundle(String name) {
        return resourceBundles.getResourceBundle(name);
    }

    @Override
    public Processor getProcessor(String name) {
        return processors.getProcessor(name);
    }

    @Override
    public List<Resource> getResources(ResourceContext context, String bundleName, ResourceType type) {
        List<Resource> res = new ArrayList<>();
        ResourceBundle rb = resourceBundles.getResourceBundle(bundleName);
        if (rb == null) {
            return res;
        }

        Map<String, Resource> all = new HashMap<>();
        // retrieve deps + filter depending on type + detect cycles
        DAG graph = new DAG();
        for (String rn : rb.getResources()) {
            Resource r = getResource(rn);
            if (r == null) {
                log.error(String.format("Unknown resource reference named '%s'", rn));
                continue;
            }
            if (type != null && !type.matches(r)) {
                continue;
            }
            graph.addVertex(rn);
            all.put(rn, r);
            all.putAll(getSubResources(graph, r, type));
        }

        for (Object rn : TopologicalSorter.sort(graph)) {
            Resource r = all.get(rn);
            r.getProcessors();
            res.add(r);
        }

        return res;
    }

    protected Map<String, Resource> getSubResources(DAG graph, Resource r, ResourceType type) {
        Map<String, Resource> res = new HashMap<String, Resource>();
        for (String dn : r.getDependencies()) {
            Resource d = getResource(dn);
            if (d == null) {
                log.error(String.format("Unknown resource dependency named '%s'", dn));
                continue;
            }
            if (type != null && !type.matches(d)) {
                continue;
            }
            res.put(dn, d);
            try {
                graph.addEdge(r.getName(), dn);
            } catch (CycleDetectedException e) {
                log.error("Cycle detected in resource dependencies: ", e);
                break;
            }
            res.putAll(getSubResources(graph, d, type));
        }
        return res;
    }

}
