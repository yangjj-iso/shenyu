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

# Ten unclaimed issues: local delivery

Selection rechecked on 2026-11-01 at about 23:16 Asia/Shanghai: the ten entries
below were open, unassigned, and had no matching open repair PR in the repository
search. Historical already-fixed/obsolete issues are excluded. These are new
local implementations, not a claim that upstream issues have been closed.

## Checkouts

- Core: branch `feat/ten-unassigned-issues-final`, based on `0b5e7c1df`
  (2.7.3-SNAPSHOT).
- Plugin store: branch `feat/7454-restore-selector-client`, based on
  `2cc3d0c`. Its external-consumer proof pins core
  `b46bf15c3a243e926994cb32c3f0a23eadc41adf` / 2.7.2-SNAPSHOT.
- All work is in isolated checkouts. The original working directory's existing
  changes were not edited.
- Initial delivery was local. The ten implementations have now been pushed and
  published as the PRs below. No upstream merge or issue closure was performed.

## Published pull requests

The core publication branches use updated upstream master `92d16c292`.
All ten feature branches and the original aggregate branches were pushed to the
yangjj-iso forks. Prerequisite-dependent PRs are drafts; their descriptions link
the prerequisites and a comparison containing only the new feature.

| Issue | Upstream PR | State / prerequisite |
| --- | --- | --- |
| #7454 | [plugin-store #33](https://github.com/apache/shenyu-plugin-store/pull/33) | Open |
| #8202 | [plugin-store #34](https://github.com/apache/shenyu-plugin-store/pull/34) | Open |
| #8200 | [ShenYu #8274](https://github.com/apache/shenyu/pull/8274) | Open |
| #8201 | [ShenYu #8272](https://github.com/apache/shenyu/pull/8272) | Open |
| #8210 | [ShenYu #8273](https://github.com/apache/shenyu/pull/8273) | Open |
| #8123 | [ShenYu #8275](https://github.com/apache/shenyu/pull/8275) | Open |
| #8205 | [ShenYu #8276](https://github.com/apache/shenyu/pull/8276) | Draft; #8274 |
| #8203 | [ShenYu #8277](https://github.com/apache/shenyu/pull/8277) | Draft; #8272 |
| #8206 | [ShenYu #8278](https://github.com/apache/shenyu/pull/8278) | Draft; #8274, #8276 |
| #8127 | [ShenYu #8279](https://github.com/apache/shenyu/pull/8279) | Draft; #8274, #8276 |

After splitting/rebasing, local owning-reactor verification passed for the
configuration family (424 tests), capability/observation family (213), HTTP
failure mapping (259), stream completion (468), Rule overrides (232) and
RabbitMQ (14). These runs overlap and should not be summed as unique tests.
Checkstyle and RAT were included. The split contract-kit branch also regenerated
its independent packaged-consumer proof: clean kit revision
`1d49d583402fd1f72ad22d7f72d7efdc8af59f86`, 18 producer tests, 4 consumer tests,
and matching hashes for all resolved ShenYu JARs.

Splitting required a duplicate-import correction for an upstream test change,
the shared fixture's existing builder-name correction for the RabbitMQ branch,
and extracting the shared Admin write-test helper so Rule overrides do not depend
on Divide. No excluded issue was added. CI is running independently on GitHub.
The initial status check found one failed job on #8273:
[Spring Cloud integration](https://github.com/apache/shenyu/actions/runs/41323434605/job/124898335416).
GitHub identifies the failed step as "Start docker compose", before test
execution. Downloading its raw log stalled, so the underlying cause is not yet
established; this is not reported as a passing CI run. Other checks have passed
or remain in progress. All ten PRs were open, mergeable, and matched their
published local branch heads when verified.

## Counted implementations

| Issue | New behavior | Commit |
| --- | --- | --- |
| [#7454](https://github.com/apache/shenyu/issues/7454) | RabbitMQ restores an identical Selector client after falling back to global settings | store `31bf032` |
| [#8200](https://github.com/apache/shenyu/issues/8200) | Scoped versioned configuration contracts, lightweight validation SPI, Admin/gateway integration and metadata | `a7c4d9e9e` |
| [#8205](https://github.com/apache/shenyu/issues/8205) | Rewrite adopts shared contracts and compatibility fixtures across Admin/runtime | `9917ad1a9` |
| [#8206](https://github.com/apache/shenyu/issues/8206) | Divide adopts shared Rule contracts with semantic/binding distinctions and parity fixtures | `f2034b655` |
| [#8202](https://github.com/apache/shenyu/issues/8202) | Packaged LEGACY plugin contract kit with an independent non-reactor consumer | store `51dba69` |
| [#8201](https://github.com/apache/shenyu/issues/8201) | Optional static capabilities, bounded inventory and conservative read-only preflight | `bc3b43ebb` |
| [#8203](https://github.com/apache/shenyu/issues/8203) | Opt-in sampled inclusive plugin lifecycle observation using bounded asynchronous delivery | `c9a23e3a1` |
| [#8210](https://github.com/apache/shenyu/issues/8210) | Typed HTTP-facing failures and optional pre-commit mappings in the global handler and common matching misses | `cea7a041e` |
| [#8123](https://github.com/apache/shenyu/issues/8123) | Separate raw AI stream completion evidence from synthesized framing; optional strict Chat completion mode | `62070685b` |
| [#8127](https://github.com/apache/shenyu/issues/8127) | Explicit allowlisted Rule generation overrides, clearing/precedence and protected target/credential policy | `694a03218` |

The retention-settings constructor repair `95b6a324b` is build support and is not
an additional issue.

Follow-up compatibility/test commits belong to the same counted issues:
`5dea00f3a` / `5fdc1ffba` (#8210 custom empty results),
`d72a60254` (#8123 asynchronous terminal-test synchronization), and
`6955cdd5a` (#8127 preservation of the existing custom resolver entry point).

## Verification

Final verification passed: **922 tests, 0 failures, 0 errors, 0 skipped** in the
selected composite regression run. It covers all web/AI Proxy/AI starter/Agent
Gateway starter test classes, shared AI observation/Rule contract tests, the real
HTTP capability pilot and lifecycle metrics adapter. Checkstyle and module RAT
checks passed in Maven verify; the final root RAT check also passed.
The earlier broad selected-reactor run exercised the remaining upstream module
suites; its asynchronous terminal-test timing assumption was corrected and the
owning suites rerun. This is not a full-repository test claim.

The completed per-feature checks include:

- Configuration/Rewrite/Divide shared fixtures at Admin write/import boundaries,
  gateway publication and real execution; metadata consistency across SQL dialects.
- A real loopback Cache → Divide → URI → HTTP client → Response fixture, including
  cache miss/hit behavior. Capability descriptors explicitly disclose the existing
  unbounded whole-body cache-miss aggregation; they do not claim it is bounded.
- Lifecycle success/error/cancellation/skip/short-circuit/fork/sampling/overflow and
  exporter-failure tests, with repeated disabled/sampled measurements against a
  pinned baseline. Measurements and their limits are in the lifecycle document.
- Global/direct error-owner behavior, custom result beans, ordered/no-match/invalid
  and throwing policies, cancellation and preservation of already-committed SSE.
- AI empty/partial/unknown/finished/error/cancelled streams, conversion, bounded
  evidence coverage, concurrent subscriptions and default/strict startup binding.
- Rule overrides: 24 shared contract cases; Admin's 208 RuleService tests including
  new create/update/import controls; real plugin/engine outgoing requests,
  credential preservation, clearing, stream precedence and immutable request state.
- RabbitMQ: 14 handler/cache tests. Test kit: 18 producer tests and 4 independent
  packaged-consumer tests, with a fresh isolated consumer repository and SHA256
  comparison of every resolved ShenYu JAR. The regenerated proof records clean
  kit revision `51dba69f88ede76f8156351864428c092a9d84ff`.

Reproduction commands use the repository wrapper. On Windows use `mvnw.cmd`
and quote dotted -D arguments in PowerShell:

```text
./mvnw -B -ntp -Phttp-capability-pilot -pl :shenyu-web,:shenyu-plugin-rewrite,:shenyu-plugin-divide,:shenyu-plugin-metrics,:shenyu-spring-boot-starter-plugin-ai-proxy,:shenyu-spring-boot-starter-plugin-agent-gateway -am verify '-Dtest=org/apache/shenyu/web/**/*Test,org/apache/shenyu/plugin/ai/proxy/**/*Test,org/apache/shenyu/springboot/starter/plugin/ai/proxy/**/*Test,org/apache/shenyu/springboot/starter/plugin/agent/gateway/**/*Test,AiRequestObservationTest,AiProxyRuleContractTest,GatewayFailureMappingTest,WebFluxResultUtilsTest,HttpCapabilityPilotTest,PluginLifecycleMetricsSinkTest' -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.javadoc.skip=true
./mvnw -B -ntp -pl :shenyu-plugin-ai-proxy,:shenyu-admin -am test -Dtest=AiProxyRuleContractTest,AiProxyPluginTest,AiProxyConfigServiceTest,AiProxyPluginHandlerTest,RuleServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.javadoc.skip=true
./mvnw -B -ntp -N apache-rat:check
```

Plugin-store proof:
`script/plugin-store/verify-legacy-contract-consumer.ps1 -CoreCheckout <pinned-core-checkout> -DependencyCache <release-dependency-cache>`.
See its generated `target/legacy-contract-proof/proof.json` and producer/consumer
logs. This proves the documented pinned pair, not a release compatibility matrix.

Tests ran on Windows with Temurin Java 21 targeting Java 17 bytecode. A Java 17
runtime, full repository build and Docker-based integration/e2e suite were not
run. Focused loopback/in-process tests do not assert production workload capacity.

## Excluded and preserved work

- #8204 gained another contributor's [PR #8270](https://github.com/apache/shenyu/pull/8270)
  after its local implementation. It is excluded from the ten and the final
  branch. Its work is retained in `feat/ten-unassigned-issues-with-8204-backup`
  and the original development branch.
- #8208 became assigned during development. Its completed local work is retained
  in `feat/8208-matching-retained`; it is excluded from the final branch/count.
- #8219 became assigned during development. Its candidate is retained in
  `feat/8219-inspection-candidate`; the mandatory forwarding performance gate
  remains incomplete. It is not a completed fix and is excluded from the count.

## Feature documentation

- [Configuration contracts](plugin-configuration-contracts.md)
- [Capability descriptors](plugin-capability-descriptors.md)
- [Lifecycle observation](plugin-lifecycle-observation.md)
- [HTTP failure mapping](gateway-failure-mapping.md)
- [AI stream completion](ai-stream-completion-evidence.md)
- [AI Rule overrides](ai-proxy-rule-overrides.md)

Rewrite/Divide fixture details and store `CONTRACT-KIT.md` describe compatibility,
lifecycle boundaries and remaining exclusions.
