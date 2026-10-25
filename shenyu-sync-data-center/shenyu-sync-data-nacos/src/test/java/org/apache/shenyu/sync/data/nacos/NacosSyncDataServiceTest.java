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

package org.apache.shenyu.sync.data.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import org.apache.shenyu.common.config.ShenyuConfig;
import org.apache.shenyu.common.constant.NacosPathConstants;
import org.apache.shenyu.sync.data.api.PluginDataSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * add test case for {@link NacosSyncDataService}.
 */
public final class NacosSyncDataServiceTest {

    private static final String SELECTOR_LIST_KEY = "namespace.selector.divide.list";

    private static final String SELECTOR_KEY = "namespace.selector.divide.selector-id";

    private static final String RULE_LIST_KEY = "namespace.rule.divide.selector-id.list";

    private static final String RULE_KEY = "namespace.rule.divide.selector-id.rule-id";

    private final Map<String, String> configs = new HashMap<>();

    private final Map<String, Listener> listeners = new HashMap<>();

    private ConfigService configService;

    private PluginDataSubscriber pluginDataSubscriber;

    private NacosSyncDataService nacosSyncDataService;

    @BeforeEach
    public void setup() throws Exception {
        configService = mock(ConfigService.class);
        pluginDataSubscriber = mock(PluginDataSubscriber.class);
        configs.put("namespace.plugin.list", "[\"divide\"]");
        configs.put(SELECTOR_LIST_KEY, "[\"selector-id\"]");
        configs.put(SELECTOR_KEY, "{\"id\":\"selector-id\",\"pluginName\":\"divide\"}");
        configs.put(RULE_LIST_KEY, "[\"rule-id\"]");
        configs.put(RULE_KEY, "{\"id\":\"rule-id\",\"selectorId\":\"selector-id\",\"pluginName\":\"divide\"}");
        when(configService.getConfigAndSignListener(anyString(), eq(NacosPathConstants.GROUP), anyLong(), any(Listener.class)))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    listeners.put(key, invocation.getArgument(3));
                    return configs.get(key);
                });
        doAnswer(invocation -> {
            listeners.remove(invocation.getArgument(0), invocation.getArgument(2));
            return null;
        }).when(configService).removeListener(anyString(), eq(NacosPathConstants.GROUP), any(Listener.class));
        ShenyuConfig shenyuConfig = new ShenyuConfig();
        shenyuConfig.setNamespace("namespace");
        nacosSyncDataService = new NacosSyncDataService(configService, pluginDataSubscriber,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), shenyuConfig);
    }

    @Test
    public void testClose() {
        nacosSyncDataService.close();
        assertTrue(listeners.isEmpty());
    }

    @Test
    public void testSelectorDeletionReleasesRuleListListener() {
        deleteSelector();

        assertFalse(listeners.containsKey(SELECTOR_KEY));
        assertFalse(listeners.containsKey(RULE_KEY));
        assertFalse(listeners.containsKey(RULE_LIST_KEY));
        assertTrue(listeners.containsKey(SELECTOR_LIST_KEY));
    }

    @Test
    public void testRecreatedSelectorRegistersRuleListAgain() throws Exception {
        deleteSelector();
        clearInvocations(pluginDataSubscriber);
        configs.put(SELECTOR_KEY, "{\"id\":\"selector-id\",\"pluginName\":\"divide\"}");
        configs.put(RULE_LIST_KEY, "[\"replacement-rule\"]");
        configs.put("namespace.rule.divide.selector-id.replacement-rule",
                "{\"id\":\"replacement-rule\",\"selectorId\":\"selector-id\",\"pluginName\":\"divide\"}");

        listeners.get(SELECTOR_LIST_KEY).receiveConfigInfo("[\"selector-id\"]");

        verify(configService, times(2)).getConfigAndSignListener(eq(RULE_LIST_KEY), eq(NacosPathConstants.GROUP), anyLong(), any(Listener.class));
        verify(pluginDataSubscriber).onRuleSubscribe(argThat(rule -> "replacement-rule".equals(rule.getId())));
    }

    private void deleteSelector() {
        configs.remove(SELECTOR_KEY);
        configs.remove(RULE_LIST_KEY);
        configs.remove(RULE_KEY);
        listeners.get(SELECTOR_KEY).receiveConfigInfo(null);
        listeners.get(RULE_KEY).receiveConfigInfo(null);
    }
}
