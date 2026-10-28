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

import org.apache.shenyu.common.constant.Constants;
import org.apache.shenyu.common.dto.PluginData;
import org.apache.shenyu.common.dto.SelectorData;
import org.apache.shenyu.common.dto.convert.rule.AiProxyHandle;
import org.apache.shenyu.common.enums.PluginEnum;
import org.apache.shenyu.plugin.ai.common.config.AiCommonConfig;
import org.apache.shenyu.plugin.ai.proxy.enhanced.protocol.AiProxyProtocolFactory;
import org.apache.shenyu.plugin.ai.proxy.enhanced.protocol.OpenAiChat;
import org.apache.shenyu.plugin.ai.proxy.enhanced.protocol.OpenAiResponses;
import org.apache.shenyu.plugin.ai.proxy.enhanced.provider.AiProxyProviderFactory;
import org.apache.shenyu.plugin.ai.proxy.enhanced.provider.OpenAi;
import org.apache.shenyu.plugin.ai.proxy.enhanced.service.AiProxyConfigService;
import org.apache.shenyu.plugin.base.utils.CacheKeyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiProxyPluginHandlerTest {

    private AiProxyPluginHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AiProxyPluginHandler(new AiProxyProviderFactory(List.of(new OpenAi())),
                new AiProxyProtocolFactory(List.of(new OpenAiChat(), new OpenAiResponses())));
    }

    @Test
    void testPluginNamed() {
        assertEquals(PluginEnum.AI_PROXY.getName(), handler.pluginNamed());
    }

    @Test
    void testHandlerSelectorCachesHandle() {
        SelectorData selector = new SelectorData();
        selector.setId("sel-1");
        selector.setHandle("{\"provider\":\"open_ai\",\"baseUrl\":\"https://api.openai.com\",\"apiKey\":\"sk-test\",\"model\":\"gpt-4\"}");

        handler.handlerSelector(selector);

        String key = CacheKeyUtils.INST.getKey("sel-1", Constants.DEFAULT_RULE);
        AiProxyHandle cached = handler.getSelectorCachedHandle().obtainHandle(key);
        assertNotNull(cached);
        assertEquals("open_ai", cached.getProvider());
        assertEquals("https://api.openai.com", cached.getBaseUrl());
        assertEquals("sk-test", cached.getApiKey());
        assertEquals("gpt-4", cached.getModel());
    }

    @Test
    void testHandlerSelectorWithNullHandle() {
        SelectorData selector = new SelectorData();
        selector.setId("sel-2");
        selector.setHandle(null);

        handler.handlerSelector(selector);

        String key = CacheKeyUtils.INST.getKey("sel-2", Constants.DEFAULT_RULE);
        assertNull(handler.getSelectorCachedHandle().obtainHandle(key));
    }

    @Test
    void testHandlerSelectorWithEmptyHandle() {
        SelectorData selector = new SelectorData();
        selector.setId("sel-3");
        selector.setHandle("");

        handler.handlerSelector(selector);

        String key = CacheKeyUtils.INST.getKey("sel-3", Constants.DEFAULT_RULE);
        assertNull(handler.getSelectorCachedHandle().obtainHandle(key));
    }

    @Test
    void testHandlerSelectorWithFallbackNormalize() {
        SelectorData selector = new SelectorData();
        selector.setId("sel-4");
        selector.setHandle("{\"provider\":\"open_ai\",\"fallbackEnabled\":\"true\",\"fallbackModel\":\"fallback-model\"}");

        handler.handlerSelector(selector);

        String key = CacheKeyUtils.INST.getKey("sel-4", Constants.DEFAULT_RULE);
        AiProxyHandle cached = handler.getSelectorCachedHandle().obtainHandle(key);
        assertNotNull(cached);
        assertNotNull(cached.getFallbackConfig());
        assertEquals("fallback-model", cached.getFallbackConfig().getModel());
    }

    @Test
    void testRemoveSelector() {
        SelectorData selector = new SelectorData();
        selector.setId("sel-5");
        selector.setHandle("{\"provider\":\"open_ai\",\"model\":\"gpt-4\"}");

        handler.handlerSelector(selector);

        String key = CacheKeyUtils.INST.getKey("sel-5", Constants.DEFAULT_RULE);
        assertNotNull(handler.getSelectorCachedHandle().obtainHandle(key));

        handler.removeSelector(selector);
        assertNull(handler.getSelectorCachedHandle().obtainHandle(key));
    }

    @Test
    void testNullPluginUpdateIsIgnored() {
        handler.handlerPlugin(null);
    }

    @Test
    void testGlobalConfigurationIsReplacedAndCopied() {
        handler.handlerPlugin(plugin("{\"provider\":\"OpenAI\",\"apiKey\":\"first-key\",\"model\":\"first-model\"}"));
        AiCommonConfig first = handler.getGlobalConfig();
        assertEquals("first-model", first.getModel());
        first.setApiKey("request-key");
        assertEquals("first-key", handler.getGlobalConfig().getApiKey());
        handler.handlerPlugin(plugin("{\"model\":\"second-model\"}"));
        assertEquals("second-model", handler.getGlobalConfig().getModel());
        assertNull(handler.getGlobalConfig().getApiKey());
        assertNull(handler.getGlobalConfig().getProvider());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "null", "{}"})
    void testClearingPluginConfigurationRemovesOldDefaults(final String config) {
        handler.handlerPlugin(plugin("{\"apiKey\":\"old-key\",\"model\":\"old-model\"}"));
        handler.handlerPlugin(plugin(config));
        AiCommonConfig cleared = new AiProxyConfigService(handler).resolvePrimaryConfig(null);
        assertNull(cleared.getApiKey());
        assertNull(cleared.getModel());
    }

    @Test
    void testDisableReenableAndRemovePlugin() {
        PluginData data = plugin("{\"model\":\"global-model\"}");
        handler.handlerPlugin(data);
        data.setEnabled(false);
        handler.handlerPlugin(data);
        assertNull(handler.getGlobalConfig());
        data.setEnabled(true);
        handler.handlerPlugin(data);
        assertEquals("global-model", handler.getGlobalConfig().getModel());
        handler.removePlugin(data);
        assertNull(handler.getGlobalConfig());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{", "[]", "{\"maxTokens\":\"invalid\"}", "{\"stream\":\"invalid\"}", "{\"stream\":1}"})
    void testInvalidPluginConfigurationPreservesDefaultsAndAllowsRecovery(final String config) {
        handler.handlerPlugin(plugin("{\"apiKey\":\"old-key\",\"model\":\"old-model\"}"));
        assertNotNull(handler.getGlobalConfig());

        assertDoesNotThrow(() -> handler.handlerPlugin(plugin(config)));

        AiCommonConfig retained = new AiProxyConfigService(handler).resolvePrimaryConfig(null);
        assertEquals("old-key", retained.getApiKey());
        assertEquals("old-model", retained.getModel());

        handler.handlerPlugin(plugin("{\"apiKey\":\"new-key\",\"model\":\"new-model\"}"));
        assertEquals("new-key", handler.getGlobalConfig().getApiKey());
        assertEquals("new-model", handler.getGlobalConfig().getModel());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"maxTokens\":0}", "{\"maxTokens\":-1}", "{\"temperature\":1e309}",
            "{\"baseUrl\":\"file:///tmp/model\"}", "{\"baseUrl\":\"https://user:secret@example.com\"}",
            "{\"baseUrl\":\"https://example.com?api_key=secret\"}", "{\"baseUrl\":\"https://example.com#secret\"}",
            "{\"baseUrl\":\"not a URI\"}", "{\"baseUrl\":\"\"}"})
    void testInvalidFieldsDoNotReplaceDefaults(final String config) {
        handler.handlerPlugin(plugin("{\"apiKey\":\"old-key\",\"model\":\"old-model\",\"maxTokens\":10}"));
        AiCommonConfig previous = handler.getGlobalConfig();

        assertDoesNotThrow(() -> handler.handlerPlugin(plugin(config)));

        assertEquals(previous, handler.getGlobalConfig());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"provider\":\"not-installed\"}", "{\"protocol\":\"not-installed\"}"})
    void testUnknownExtensionDoesNotReplaceDefaults(final String config) {
        handler.handlerPlugin(plugin("{\"apiKey\":\"old-key\",\"model\":\"old-model\"}"));
        AiCommonConfig previous = handler.getGlobalConfig();

        assertDoesNotThrow(() -> handler.handlerPlugin(plugin(config)));

        assertEquals(previous, handler.getGlobalConfig());
    }

    @Test
    void testBlankNamesKeepDefaultProviderAndProtocolDetectionSemantics() {
        handler.handlerPlugin(plugin("{\"provider\":\" \",\"protocol\":\" \",\"model\":\"model\"}"));
        assertEquals("model", handler.getGlobalConfig().getModel());
    }

    @Test
    void testRegisteredNamesCanUseProtocolConversion() {
        // OpenAi supports Chat; the engine can route Responses through a registered converter.
        handler.handlerPlugin(plugin("{\"provider\":\"OpenAI\",\"protocol\":\"openai-responses\"}"));
        assertEquals("openai-responses", handler.getGlobalConfig().getProtocol());
    }

    @Test
    void testRejectedInitialConfigurationDoesNotActivateDefaults() {
        handler.handlerPlugin(plugin("{\"maxTokens\":0}"));
        assertNull(handler.getGlobalConfig());
    }

    @Test
    void testDisableWithInvalidPayloadStillRevokesDefaults() {
        handler.handlerPlugin(plugin("{\"apiKey\":\"old-key\"}"));
        PluginData disabled = plugin("{");
        disabled.setEnabled(false);
        handler.handlerPlugin(disabled);
        assertNull(handler.getGlobalConfig());

        disabled.setEnabled(true);
        handler.handlerPlugin(disabled);
        assertNull(handler.getGlobalConfig());
    }

    @Test
    void testPartialDefaultsAndSelectorSettingsAreCombined() {
        handler.handlerPlugin(plugin("{\"maxTokens\":10,\"temperature\":0.5}"));
        AiProxyHandle selector = new AiProxyHandle();
        selector.setBaseUrl("https://example.com/v1");
        selector.setApiKey("selector-key");
        selector.setModel("selector-model");
        selector.setTemperature(null);

        AiCommonConfig effective = new AiProxyConfigService(handler).resolvePrimaryConfig(selector);

        assertEquals(10, effective.getMaxTokens());
        assertEquals(0.5, effective.getTemperature());
        assertEquals("selector-model", effective.getModel());
        assertEquals("selector-key", effective.getApiKey());
    }

    @Test
    void testExplicitSelectorSettingsOverridePartialDefaults() {
        handler.handlerPlugin(plugin("{\"maxTokens\":10,\"temperature\":0.5}"));
        AiProxyHandle selector = new AiProxyHandle();
        selector.setMaxTokens(20);
        selector.setTemperature(0.8);

        AiCommonConfig effective = new AiProxyConfigService(handler).resolvePrimaryConfig(selector);

        assertEquals(20, effective.getMaxTokens());
        assertEquals(0.8, effective.getTemperature());
        assertEquals(10, handler.getGlobalConfig().getMaxTokens());
        assertEquals(0.5, handler.getGlobalConfig().getTemperature());
    }

    @Test
    void testLegacyStringValuesRemainSupported() {
        handler.handlerPlugin(plugin("{\"temperature\":\"0.5\",\"maxTokens\":\"1000\",\"stream\":\"false\"}"));
        AiCommonConfig config = handler.getGlobalConfig();
        assertEquals(0.5, config.getTemperature());
        assertEquals(1000, config.getMaxTokens());
        assertEquals(false, config.getStream());
    }

    @Test
    void testPreviouslyResolvedRequestKeepsItsSnapshot() {
        handler.handlerPlugin(plugin("{\"apiKey\":\"first-key\",\"model\":\"first-model\"}"));
        AiCommonConfig request = new AiProxyConfigService(handler).resolvePrimaryConfig(null);

        handler.handlerPlugin(plugin("{\"apiKey\":\"second-key\",\"model\":\"second-model\"}"));
        handler.removePlugin(null);

        assertEquals("first-key", request.getApiKey());
        assertEquals("first-model", request.getModel());
        assertNull(handler.getGlobalConfig());
    }

    @Test
    void testConcurrentReadersObserveWholeSnapshots() throws Exception {
        String first = "{\"apiKey\":\"first\",\"model\":\"first\"}";
        String second = "{\"apiKey\":\"second\",\"model\":\"second\"}";
        handler.handlerPlugin(plugin(first));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> writer = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 1000; i++) {
                    handler.handlerPlugin(plugin(i % 2 == 0 ? first : second));
                }
                return null;
            });
            start.countDown();
            for (int i = 0; i < 1000; i++) {
                AiCommonConfig snapshot = handler.getGlobalConfig();
                assertEquals(snapshot.getApiKey(), snapshot.getModel());
            }
            writer.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testHandlerInstancesDoNotShareGlobalConfiguration() {
        AiProxyPluginHandler other = new AiProxyPluginHandler();
        handler.handlerPlugin(plugin("{\"model\":\"first-model\"}"));
        assertNull(other.getGlobalConfig());
        other.handlerPlugin(plugin("{\"model\":\"second-model\"}"));
        assertEquals("first-model", handler.getGlobalConfig().getModel());
        assertEquals("second-model", other.getGlobalConfig().getModel());
        handler.removePlugin(null);
        assertEquals("second-model", other.getGlobalConfig().getModel());
    }

    private PluginData plugin(final String config) {
        PluginData data = new PluginData();
        data.setName(PluginEnum.AI_PROXY.getName());
        data.setConfig(config);
        return data;
    }
}
