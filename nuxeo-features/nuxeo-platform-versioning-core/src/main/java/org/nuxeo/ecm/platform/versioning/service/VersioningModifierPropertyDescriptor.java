/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.versioning.service;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;


/***
 *
 * @author <a href="mailto:bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 *
 */
@XObject("property")
public class VersioningModifierPropertyDescriptor {

    @XNode("@schema")
    private String schema;

    @XNode("@action")
    private String action;

    @XContent
    private String fieldname;

    public String getAction() {
        return action.trim();
    }

    public String getFieldname() {
        return fieldname.trim();
    }

    public String getSchema() {
        return schema.trim();
    }

}