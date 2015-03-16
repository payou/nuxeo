package org.nuxeo.elasticsearch.http.readonly;

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
 *     bdelbosc
 */

import java.io.IOException;
import java.security.Principal;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@Path("/es")
@WebObject(type = "es")
@Produces(MediaType.APPLICATION_JSON)
public class Main extends ModuleRoot {

    private static final Log log = LogFactory.getLog(Main.class);

    private static final String DEFAULT_ES_BASE_URL = "http://localhost:9200/";

    private static final RequestValidator validator = new RequestValidator();

    @GET
    @Path("{indices}/{types}/_search")
    @Consumes("application/x-www-form-urlencoded")
    public String searchWithPayLoad(@PathParam("indices") String indices, @PathParam("types") String types,
            @Context UriInfo uriInf, MultivaluedMap<String, String> formParams) throws IOException, JSONException {
        NuxeoPrincipal principal = getPrincipal();
        String payload = formParams.keySet().iterator().next();
        indices = validator.getIndices(indices);
        types = validator.getTypes(indices, types);
        SearchRequestFilter req = new SearchRequestFilter(principal, indices, types,
                uriInf.getRequestUri().getRawQuery(), payload);
        log.warn(req);
        return HttpClient.get(getElasticsearchBaseUrl() + req.getUrl(), req.getPayload());
    }

    @GET
    @Path("{indices}/{types}/_search")
    public String search(@PathParam("indices") String indices, @PathParam("types") String types, @Context UriInfo uriInf)
            throws IOException, JSONException {
        NuxeoPrincipal principal = getPrincipal();
        indices = validator.getIndices(indices);
        types = validator.getTypes(indices, types);
        SearchRequestFilter req = new SearchRequestFilter(principal, indices, types,
                uriInf.getRequestUri().getRawQuery(), null);
        log.warn(req);
        return HttpClient.get(getElasticsearchBaseUrl() + req.getUrl(), req.getPayload());
    }

    @GET
    @Path("{indices}/{types}/{documentId: [a-zA-Z0-9\\-]+}")
    public String getDocument(@PathParam("indices") String indices, @PathParam("types") String types,
            @PathParam("documentId") String documentId, @Context UriInfo uriInf) throws IOException, JSONException {
        NuxeoPrincipal principal = getPrincipal();
        indices = validator.getIndices(indices);
        types = validator.getTypes(indices, types);
        validator.checkValidDocumentId(documentId);
        DocRequestFilter req = new DocRequestFilter(principal, indices, types, documentId,
                uriInf.getRequestUri().getRawQuery());
        log.warn(req);
        if (principal.isAdministrator()) {
            String docAcl = HttpClient.get(getElasticsearchBaseUrl() + req.getCheckAccessUrl());
            validator.hasAccess(principal, docAcl);
        }
        return HttpClient.get(getElasticsearchBaseUrl() + req.getUrl());
    }

    protected String getElasticsearchBaseUrl() {
        // TODO: make ES base url configurable
        return DEFAULT_ES_BASE_URL;
    }

    public @NotNull NuxeoPrincipal getPrincipal() {
        Principal principal = ctx.getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException("No principal found");
        }
        return (NuxeoPrincipal) principal;
    }

}
