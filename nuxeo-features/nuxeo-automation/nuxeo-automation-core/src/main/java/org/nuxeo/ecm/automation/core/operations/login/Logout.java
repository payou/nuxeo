/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.login;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = Logout.ID, category = Constants.CAT_USERS_GROUPS, label = "Logout", description = "Perform a logout. This should be used only after using the Login As operation to restore original login. This is a void operations - the input will be returned back as the output.")
public class Logout {

    public static final String ID = "Auth.Logout";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public void run() throws OperationException {
        ctx.getLoginStack().pop();
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws OperationException {
        run();
        // refetch the input document if any using the new session
        // otherwise using document methods that are delegating the call to the
        // session that created the document will call the old session.
        return ctx.getCoreSession().getDocument(doc.getRef());
    }

}
