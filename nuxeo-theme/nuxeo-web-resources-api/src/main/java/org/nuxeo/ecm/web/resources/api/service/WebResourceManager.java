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
package org.nuxeo.ecm.web.resources.api.service;

import java.util.List;

import org.nuxeo.ecm.web.resources.api.Processor;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.ResourceContext;
import org.nuxeo.ecm.web.resources.api.ResourceType;

/**
 * Service for web resources retrieval.
 *
 * @since 7.2
 */
public interface WebResourceManager {

    /**
     * Returns a registered resource with given name, or null if not found.
     * <p>
     * Referenced resource can either be a static resource or a style.
     */
    Resource getResource(String name);

    /**
     * Returns a registered resource bundle with given name, or null if not found.
     */
    ResourceBundle getResourceBundle(String name);

    /**
     * Returns the corresponding processor with given name, or null if not found.
     */
    Processor getProcessor(String name);

    /**
     * Returns the ordered list of resources for given bundle name, filtered using given type.
     * <p>
     */
    List<Resource> getResources(ResourceContext context, String bundleName, ResourceType type);

}