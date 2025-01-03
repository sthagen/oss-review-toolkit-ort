/*
 * Copyright (C) 2024 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.plugins.advisors.blackduck

import io.kotest.core.spec.style.WordSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot

import java.time.Instant

import org.ossreviewtoolkit.advisor.normalizeVulnerabilityData
import org.ossreviewtoolkit.model.AdvisorResult
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.readValue
import org.ossreviewtoolkit.model.toYaml
import org.ossreviewtoolkit.utils.common.Os
import org.ossreviewtoolkit.utils.test.getAssetFile
import org.ossreviewtoolkit.utils.test.identifierToPackage

class BlackDuckFunTest : WordSpec({
    /**
     * To run the test against a real instance, and / or to re-record the responses:
     *
     * 1. Set the BLACK_DUCK_SERVER_URL and BLACK_DUCK_API_TOKEN environment variables.
     * 2. Delete 'recorded-responses.json'.
     * 3. Run the functional test.
     */
    val serverUrl = Os.env["BLACK_DUCK_SERVER_URL"]
    val apiToken = Os.env["BLACK_DUCK_API_TOKEN"]
    val componentServiceClient = ResponseCachingComponentServiceClient(
        overrideFile = getAssetFile("recorded-responses.json"),
        serverUrl = serverUrl,
        apiToken = apiToken
    )

    val blackDuck = BlackDuck(BlackDuckFactory.descriptor, componentServiceClient)

    afterEach { componentServiceClient.flush() }

    "retrievePackageFindings()" should {
        "return the vulnerabilities for the supported ecosystems" {
            val packages = setOf(
                // TODO: Add hackage / pod
                "Crate::sys-info:0.7.0",
                "Gem::rack:2.0.4",
                "Maven:com.jfinal:jfinal:1.4",
                "NPM::rebber:1.0.0",
                "NuGet::Bunkum:4.0.0",
                "Pub::http:0.13.1",
                "PyPI::django:3.2"
            ).mapTo(mutableSetOf()) {
                identifierToPackage(it)
            }

            val packageFindings = blackDuck.retrievePackageFindings(packages).mapKeys { it.key.id.toCoordinates() }

            packageFindings.keys shouldContainExactlyInAnyOrder packages.map { it.id.toCoordinates() }
            packageFindings.keys.forAll { id ->
                packageFindings.getValue(id).vulnerabilities shouldNot beEmpty()
            }
        }

        "return the expected result for the given package(s)" {
            val expectedResult = getAssetFile("retrieve-package-findings-expected-result.yml")
                .readValue<Map<Identifier, AdvisorResult>>()
            val packages = setOf(
                // Package using CVSS 3.1 vector:
                "Crate::sys-info:0.7.0"
                // Todo: Add a package using CVSS 2 vector:
            ).mapTo(mutableSetOf()) {
                identifierToPackage(it)
            }

            val packageFindings = blackDuck.retrievePackageFindings(packages).mapKeys { it.key.id }

            packageFindings.patchTimes().toYaml().patchServerUrl(serverUrl) shouldBe
                expectedResult.patchTimes().toYaml()
        }
    }
})

internal fun String.patchServerUrl(serverUrl: String?) =
    serverUrl?.let { replace(it, "https://BLACK_DUCK_SERVER_HOST") } ?: this

private fun Map<Identifier, AdvisorResult>.patchTimes(): Map<Identifier, AdvisorResult> =
    mapValues { (_, advisorResult) ->
        advisorResult.normalizeVulnerabilityData().copy(
            summary = advisorResult.summary.copy(
                startTime = Instant.EPOCH,
                endTime = Instant.EPOCH
            )
        )
    }
