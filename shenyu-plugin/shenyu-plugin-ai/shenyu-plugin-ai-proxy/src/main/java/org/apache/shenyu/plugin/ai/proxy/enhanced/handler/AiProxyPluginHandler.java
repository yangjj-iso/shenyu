/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.plugin.ai.proxy.enhanced.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.common.constant.Constants;
import org.apache.shenyu.common.dto.PluginData;
import org.apache.shenyu.common.dto.SelectorData;
import org.apache.shenyu.common.dto.convert.rule.AiProxyHandle;
import org.apache.shenyu.common.enums.PluginEnum;
import org.apache.shenyu.common.exception.ShenyuException;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.plugin.ai.common.config.AiCommonConfig;
import org.apache.shenyu.plugin.ai.proxy.enhanced.cache.AiProxyApiKeyCache;
import org.apache.shenyu.plugin.ai.proxy.enhanced.protocol.AiProxyProtocolFactory;
import org.apache.shenyu.plugin.ai.proxy.enhanced.provider.AiProxyProviderFactory;
import org.apache.shenyu.plugin.ai.proxy.enhanced.service.AiProxyConfigValidator;
import org.apache.shenyu.plugin.base.cache.CommonHandleCache;
import org.apache.shenyu.plugin.base.handler.PluginDataHandler;
import org.apache.shenyu.plugin.base.utils.CacheKeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * this is ai proxy plugin handler.
 */
public class AiProxyPluginHandler implements PluginDataHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AiProxyPluginHandler.class);

    private final CommonHandleCache<String, AiProxyHandle> selectorCachedHandle = new CommonHandleCache<>();

    private final AiProxyProviderFactory providerFactory;

    private final AiProxyProtocolFactory protocolFactory;

    private volatile AiCommonConfig globalConfig;

    /**
     * Create a standalone handler with portable-field validation only.
     * Use the factory-aware constructor to validate installed extension names before activation.
     */
    public AiProxyPluginHandler() {
        providerFactory = null;
        protocolFactory = null;
    }

    /**
     * Create a handler that also validates configured names against installed extensions.
     *
     * @param providerFactory installed provider registry
     * @param protocolFactory installed protocol registry
     */
    public AiProxyPluginHandler(final AiProxyProviderFactory providerFactory, final AiProxyProtocolFactory protocolFactory) {
        this.providerFactory = Objects.requireNonNull(providerFactory);
        this.protocolFactory = Objects.requireNonNull(protocolFactory);
    }

    @Override
    public synchronized void handlerPlugin(final PluginData pluginData) {
        if (Objects.isNull(pluginData)) {
            return;
        }
        // Explicit disable/clear is authoritative, including credential revocation.
        if (Boolean.FALSE.equals(pluginData.getEnabled()) || StringUtils.isBlank(pluginData.getConfig())) {
            globalConfig = null;
            return;
        }
        try {
            JsonElement json = GsonUtils.getInstance().fromJson(pluginData.getConfig(), JsonElement.class);
            if (Objects.isNull(json) || json.isJsonNull() || json.isJsonObject() && json.getAsJsonObject().entrySet().isEmpty()) {
                globalConfig = null;
                return;
            }
            if (!json.isJsonObject()) {
                throw new IllegalArgumentException("AI proxy defaults must be a JSON object");
            }
            JsonElement stream = json.getAsJsonObject().get("stream");
            if (Objects.nonNull(stream) && !stream.isJsonNull()
                    && (!stream.isJsonPrimitive() || !("true".equalsIgnoreCase(stream.getAsString()) || "false".equalsIgnoreCase(stream.getAsString())))) {
                throw new IllegalArgumentException("AI stream must be boolean");
            }
            AiCommonConfig candidate = GsonUtils.getInstance().fromJson(json.toString(), AiCommonConfig.class);
            AiProxyConfigValidator.validate(candidate);
            if (Objects.nonNull(providerFactory) && StringUtils.isNotBlank(candidate.getProvider())) {
                providerFactory.getProvider(candidate.getProvider());
            }
            if (Objects.nonNull(protocolFactory) && StringUtils.isNotBlank(candidate.getProtocol())) {
                protocolFactory.getProtocol(candidate.getProtocol());
            }
            globalConfig = candidate;
        } catch (JsonParseException | IllegalArgumentException | ShenyuException e) {
            // Do not log the payload or exception: either may contain credentials.
            LOG.warn("Rejected invalid AI proxy defaults; retaining the previous configuration");
        }
    }

    @Override
    public synchronized void removePlugin(final PluginData pluginData) {
        globalConfig = null;
    }

    /**
     * Return a request-local copy of the latest plugin defaults.
     *
     * @return global configuration, or null when cleared or disabled
     */
    public AiCommonConfig getGlobalConfig() {
        AiCommonConfig snapshot = globalConfig;
        return Objects.isNull(snapshot) ? null : new AiCommonConfig(snapshot);
    }

    @Override
    public void handlerSelector(final SelectorData selectorData) {
        // Do NOT remove AiProxyApiKeyCache here. Admin will push updated AI_PROXY_API_KEY events
        // with refreshed realApiKey after selector changes. Removing here introduces a window of misses.
        if (Objects.isNull(selectorData.getHandle()) || selectorData.getHandle().isEmpty()) {
            // Clear stale cached handle when selector handle is removed/cleared
            selectorCachedHandle
                    .removeHandle(CacheKeyUtils.INST.getKey(selectorData.getId(), Constants.DEFAULT_RULE));
            return;
        }
        AiProxyHandle aiProxyHandle = GsonUtils.getInstance().fromJson(selectorData.getHandle(), AiProxyHandle.class);
        aiProxyHandle.normalize();
        selectorCachedHandle
                .cachedHandle(CacheKeyUtils.INST.getKey(selectorData.getId(), Constants.DEFAULT_RULE), aiProxyHandle);
    }

    @Override
    public void removeSelector(final SelectorData selectorData) {
        AiProxyApiKeyCache.getInstance().removeBySelectorId(selectorData.getId());
        selectorCachedHandle
                .removeHandle(CacheKeyUtils.INST.getKey(selectorData.getId(), Constants.DEFAULT_RULE));
    }

    @Override
    public String pluginNamed() {
        return PluginEnum.AI_PROXY.getName();
    }

    /**
     * Gets selector cached handle.
     *
     * @return the selector cached handle
     */
    public CommonHandleCache<String, AiProxyHandle> getSelectorCachedHandle() {
        return selectorCachedHandle;
    }
}
