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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime Component implementing the {@link BatchManager} service
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class BatchManagerComponent extends DefaultComponent implements
        BatchManager {

    protected ConcurrentHashMap<String, Batch> batches = new ConcurrentHashMap<String, Batch>();

    protected static final String DEFAULT_CONTEXT = "None";

    protected static final Log log = LogFactory.getLog(BatchManagerComponent.class);

    static {
        ComplexTypeJSONDecoder.registerBlobDecoder(new JSONBatchBlobDecoder());
    }

    public String initBatch(String batchId, String contextName) {
        Batch batch = initBatchInternal(batchId, contextName);
        return batch.id;
    }

    protected Batch initBatchInternal(String batchId, String contextName) {
        if (batchId == null || batchId.isEmpty()) {
            batchId = "batchId-" + UUID.randomUUID().toString();
        }
        if (contextName == null || contextName.isEmpty()) {
            contextName = DEFAULT_CONTEXT;
        }

        Batch newBatch = new Batch(batchId);
        Batch existingBatch = batches.putIfAbsent(batchId, newBatch);

        if (existingBatch != null) {
            return existingBatch;
        } else {
            return newBatch;
        }
    }

    public void addStream(String batchId, String idx, InputStream is,
            String name, String mime) throws IOException {
        Batch batch = batches.get(batchId);
        if (batch == null) {
            batch = initBatchInternal(batchId, null);
        }
        batch.addStream(idx, is, name, mime);
    }

    public List<Blob> getBlobs(String batchId) {
        return getBlobs(batchId, 0);
    }

    public List<Blob> getBlobs(String batchId, int timeoutS) {
        Batch batch = batches.get(batchId);
        if (batch == null) {
            log.error("Unable to find batch with id " + batchId);
            return Collections.emptyList();
        }
        return batch.getBlobs(timeoutS);
    }

    public Blob getBlob(String batchId, String fileId) {
        return getBlob(batchId, fileId, 0);
    }

    public Blob getBlob(String batchId, String fileId, int timeoutS) {
        Batch batch = batches.get(batchId);
        if (batch == null) {
            log.error("Unable to find batch with id " + batchId);
            return null;
        }
        return batch.getBlob(fileId, timeoutS);
    }

    public void clean(String batchId) {
        Batch batch = batches.get(batchId);
        if (batch != null) {
            batch.clear();
            batches.remove(batchId);
        }
    }

    @Override
    public Object execute(String batchId, String chainOrOperationId,
            CoreSession session, Map<String, Object> contextParams,
            Map<String, Object> operationParams) throws ClientException {
        List<Blob> blobs = getBlobs(batchId, getUploadWaitTimeout());
        if (blobs == null) {
            String message = String.format(
                    "Unable to find batch associated with id '%s'", batchId);
            log.error(message);
            throw new ClientException(message);
        }

        if (contextParams == null) {
            contextParams = new HashMap<>();
        }
        if (operationParams == null) {
            operationParams = new HashMap<>();
        }

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(new BlobList(blobs));
        ctx.putAll(contextParams);

        try {
            Object result = null;
            AutomationService as = Framework.getLocalService(AutomationService.class);
            if (chainOrOperationId.startsWith("Chain.")) {
                result = as.run(ctx, chainOrOperationId.substring(6));
            } else {
                OperationChain chain = new OperationChain("operation");
                OperationParameters params = new OperationParameters(
                        chainOrOperationId, operationParams);
                chain.add(params);
                result = as.run(ctx, chain);
            }
            return result;
        } catch (Exception e) {
            log.error("Error while executing automation batch ", e);
            throw ClientException.wrap(e);
        }
    }

    protected int getUploadWaitTimeout() {
        String t = Framework.getProperty("org.nuxeo.batch.upload.wait.timeout",
                "5");
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            log.error("Wrong number format for upload wait timeout property", e);
            return 5;
        }
    }

    @Override
    public Object executeAndClean(String batchId, String chainOrOperationId,
            CoreSession session, Map<String, Object> contextParams,
            Map<String, Object> operationParams) throws ClientException {
        try {
            return execute(batchId, chainOrOperationId, session, contextParams,
                    operationParams);
        } finally {
            clean(batchId);
        }
    }
}