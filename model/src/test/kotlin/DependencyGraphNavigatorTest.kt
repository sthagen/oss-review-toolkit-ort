/*
 * Copyright (C) 2021 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.types.shouldBeTypeOf

import org.ossreviewtoolkit.utils.test.readOrtResult

class DependencyGraphNavigatorTest : AbstractDependencyNavigatorTest() {
    override val resultFileName = "/sbt-multi-project-example-graph.yml"
    override val resultWithIssuesFileName = "/result-with-issues-graph.yml"

    init {
        "init" should {
            "fail if there is no analyzer result" {
                shouldThrow<IllegalArgumentException> {
                    DependencyGraphNavigator(OrtResult.EMPTY)
                }
            }

            "fail if the map with dependency graphs is empty" {
                val result = OrtResult.EMPTY.copy(
                    analyzer = AnalyzerRun.EMPTY.copy(
                        result = AnalyzerResult.EMPTY
                    )
                )

                shouldThrow<IllegalArgumentException> {
                    DependencyGraphNavigator(result)
                }
            }
        }

        "getInternalId" should {
            "return the underlying graph node" {
                val result = readOrtResult(resultFileName)
                val testProject = result.getProjects().first()

                val node = navigator.directDependencies(testProject, "test").first()

                node.getInternalId().shouldBeTypeOf<DependencyGraphNode>()
            }
        }
    }
}
