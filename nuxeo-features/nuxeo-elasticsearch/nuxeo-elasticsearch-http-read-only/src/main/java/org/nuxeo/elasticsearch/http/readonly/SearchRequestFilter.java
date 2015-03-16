package org.nuxeo.elasticsearch.http.readonly;/*
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
 *     Benoit Delbosc
 */

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.security.SecurityService;

import com.sun.istack.NotNull;

/**
 * @since 7.3
 */
public class SearchRequestFilter {

    private static final String MATCH_ALL = "{\"query\": {\"match_all\": {}}}";

    private static final String QUERY_STRING = "{\"query\":{\"query_string\":{\"query\":\"%s\"}}}";

    private String payload;

    private String rawQuery;

    private final String types;

    private final String indices;

    private final NuxeoPrincipal principal;

    private String url;

    private String filteredPayload;

    public SearchRequestFilter(NuxeoPrincipal principal, String indices, String types, String rawQuery, String payload) {
        this.indices = indices;
        this.types = types;
        this.principal = principal;
        this.rawQuery = rawQuery;
        this.payload = payload;
        if (payload == null && !principal.isAdministrator()) {
            // here we turn the UriSearch query_string into a body search
            extractPayloadFromQuery();
        }
    }

    @Override
    public String toString() {
        if (payload == null) {
            return "Uri Search: " + getUrl() + " user: " + principal;
        }
        try {
            return "Body Search: " + getUrl() + " user: " + principal + " payload: " + getPayload();
        } catch (JSONException e) {
            return "Body Search: " + getUrl() + " user: " + principal + " invalid JSON payload: " + e.getMessage();
        }
    }

    public @NotNull String getUrl() {
        if (url == null) {
            url = "/" + indices + "/" + types + "/_search";
            if (rawQuery != null) {
                url += "?" + rawQuery;
            }
        }
        return url;
    }

    public String getPayload() throws JSONException {
        if (principal.isAdministrator()) {
            return payload;
        }
        if (filteredPayload == null) {
            String[] principals = SecurityService.getPrincipalsToCheck(principal);
            JSONObject payloadJson = new JSONObject(payload);
            JSONObject query;
            if (payloadJson.has("query")) {
                query = payloadJson.getJSONObject("query");
                payloadJson.remove("query");
            } else {
                query = new JSONObject("{\"match_all\":{}}");
            }
            JSONObject filter = new JSONObject().put("terms", new JSONObject().put("ecm:acl", principals));
            JSONObject newQuery = new JSONObject().put("filtered",
                    new JSONObject().put("query", query).put("filter", filter));
            payloadJson.put("query", newQuery);
            filteredPayload = payloadJson.toString();
        }
        return filteredPayload;
    }

    private void extractPayloadFromQuery() {
        Map<String, String> qm = getQueryMap();
        String queryString = qm.remove("q");
        if (queryString == null) {
            payload = MATCH_ALL;
            return;
        }
        payload = String.format(QUERY_STRING, queryString);
        setRawQuery(qm);
    }

    private Map<String, String> getQueryMap() {
        String[] params = rawQuery.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    private void setRawQuery(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }
        rawQuery = sb.toString();
    }
}
