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

package org.apache.shenyu.plugin.ai.proxy.enhanced.service;

import org.apache.shenyu.plugin.ai.common.config.AiCommonConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProxyConfigValidatorTest {

    @Test
    void testAbsentAndPartialDefaultsAreValid() {
        assertDoesNotThrow(() -> AiProxyConfigValidator.validate(null));
        AiCommonConfig config = new AiCommonConfig();
        assertDoesNotThrow(() -> AiProxyConfigValidator.validate(config));
        config.setMaxTokens(1);
        config.setTemperature(0.5);
        assertDoesNotThrow(() -> AiProxyConfigValidator.validate(config));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://localhost:8080/v1", "https://[::1]/v1/chat/completions"})
    void testValidEndpoints(final String endpoint) {
        AiCommonConfig config = new AiCommonConfig();
        config.setBaseUrl(endpoint);
        assertDoesNotThrow(() -> AiProxyConfigValidator.validate(config));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "relative/path", "file:///tmp/model", "https://secret@example.com", "https://example.com?key=secret",
            "https://example.com#secret", "https://example.com/secret value"})
    void testInvalidEndpointsHaveSafeDiagnostics(final String endpoint) {
        AiCommonConfig config = new AiCommonConfig();
        config.setBaseUrl(endpoint);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> AiProxyConfigValidator.validate(config));
        assertFalse(error.getMessage().contains("secret"));
        assertNull(error.getCause());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void testNonPositiveTokenLimitIsRejected(final int maxTokens) {
        AiCommonConfig config = new AiCommonConfig();
        config.setMaxTokens(maxTokens);
        assertThrows(IllegalArgumentException.class, () -> AiProxyConfigValidator.validate(config));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void testNonFiniteTemperatureIsRejected(final double temperature) {
        AiCommonConfig config = new AiCommonConfig();
        config.setTemperature(temperature);
        assertThrows(IllegalArgumentException.class, () -> AiProxyConfigValidator.validate(config));
    }
}
