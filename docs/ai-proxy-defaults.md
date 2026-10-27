<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
-->

# AI Proxy default configuration updates

AI Proxy parses and validates a candidate before publishing plugin defaults.
Malformed JSON, invalid supplied fields, and unavailable provider/protocol names
leave the last valid defaults unchanged. A rejected initial update leaves no
active defaults. Rejection logs do not include the configuration, identifiers,
credentials, or parser exception.

The update contract is:

- A valid object **replaces**, rather than patches, the previous defaults. Omitted
  credentials and other fields are not inherited from the previous object.
- Null/blank configuration, JSON `null`, and an empty object explicitly clear
  defaults. Disabling or removing the plugin also clears them, even when its
  accompanying configuration is malformed or refers to unavailable extensions.
- A malformed replacement is **not** a credential-revocation signal. To revoke
  defaults, explicitly clear or disable the plugin; do not rely on an invalid
  replacement to erase credentials. This does not revoke independent selector
  credentials or proxy-key mappings, or terminate requests already in flight.
- Optional global fields need not be supplied. Selectors may complete them.
  Supplied `maxTokens` must be positive, `temperature` finite, and `baseUrl` an
  HTTP(S) endpoint with a host and without userinfo, query, or fragment.
  Provider-specific temperature ranges remain the provider's responsibility.
- The existing numeric strings and `"true"`/`"false"` strings emitted by Admin
  remain supported. Other `stream` values are rejected.
- The Spring Boot starter validates nonblank provider/protocol names against the
  installed registries, including custom extensions and provider aliases. Blank
  names keep existing default-provider/protocol-detection semantics. Names are
  checked independently: a registered protocol converter can make a provider and
  client-protocol pair usable even when the provider does not natively speak it.

Each reader obtains a copy of one published defaults object. Updates/removal are
serialized; invalid candidates never mutate the active object. A request that
already resolved its configuration retains that copy. This is **not** a
transactional snapshot of selector, credential, discovery, or other live caches.

The engine validates the selected effective configuration after merging and
credential resolution, before the actual upstream attempt. Required fields and
provider/protocol compatibility still depend on the selected provider and
request. Fallback is validated when selected, not eagerly: an unused invalid
fallback does not prevent a valid primary request.

For manual construction, use
`AiProxyPluginHandler(AiProxyProviderFactory, AiProxyProtocolFactory)` to get the
same extension-name checks as the starter. The legacy no-argument constructor
retains portable-field validation but cannot validate an application registry it
has not been given; the engine still validates names before use.
