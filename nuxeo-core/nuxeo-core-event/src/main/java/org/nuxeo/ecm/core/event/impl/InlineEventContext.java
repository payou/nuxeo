/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.security.Principal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.Event.Flag;

public class InlineEventContext extends EventContextImpl {

    private static final long serialVersionUID = 1L;

    protected boolean boundToCoreSession = false;

    public InlineEventContext(Principal principal, Map<String, Serializable> properties) {
        this(null, principal, properties);
    }

    public InlineEventContext(CoreSession session, Principal principal, Map<String, Serializable> properties) {
        super(session, principal);
        setProperties(properties);
        boundToCoreSession = session != null;
    }

    @Override
    public Event newEvent(String name) {
        Set<Flag> flags = EnumSet.noneOf(Flag.class);
        if (!boundToCoreSession) {
            flags.add(Flag.INLINE);
        }
        return newEvent(name, flags);
    }

}