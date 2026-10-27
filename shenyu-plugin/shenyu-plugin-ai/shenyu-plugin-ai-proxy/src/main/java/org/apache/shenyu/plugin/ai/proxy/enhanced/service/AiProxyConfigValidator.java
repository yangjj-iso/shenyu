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

import java.net.URI;
import java.util.Objects;

/**
 * Validate portable AI configuration fields without requiring complete global defaults.
 */
public final class AiProxyConfigValidator {

    private AiProxyConfigValidator() {
    }

    /**
     * Validate fields that are supplied by defaults or a resolved configuration.
     * Provider-specific requirements are checked by the engine after selector merging
     * and proxy credential resolution, before starting an upstream request.
     *
     * @param config optional configuration
     */
    public static void validate(final AiCommonConfig config) {
        if (Objects.isNull(config)) {
            return;
        }
        if (Objects.nonNull(config.getMaxTokens()) && config.getMaxTokens() <= 0) {
            throw new IllegalArgumentException("AI maxTokens must be positive");
        }
        if (Objects.nonNull(config.getTemperature()) && !Double.isFinite(config.getTemperature())) {
            throw new IllegalArgumentException("AI temperature must be finite");
        }
        if (Objects.nonNull(config.getBaseUrl())) {
            final URI base;
            try {
                base = URI.create(config.getBaseUrl());
            } catch (IllegalArgumentException e) {
                // URI parsing errors contain the input; do not retain a credential-bearing cause.
                throw new IllegalArgumentException("AI baseUrl must be a valid HTTP(S) endpoint");
            }
            if (!("http".equalsIgnoreCase(base.getScheme()) || "https".equalsIgnoreCase(base.getScheme()))
                    || Objects.isNull(base.getHost()) || Objects.nonNull(base.getRawQuery())
                    || Objects.nonNull(base.getRawFragment()) || Objects.nonNull(base.getRawUserInfo())) {
                throw new IllegalArgumentException("AI baseUrl must be an HTTP(S) endpoint without credentials, query or fragment");
            }
        }
    }
}
