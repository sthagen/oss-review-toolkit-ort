/*
 * Copyright (C) 2017 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.plugins.packagemanagers.maven.utils

import io.kotest.core.spec.style.WordSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe

import org.ossreviewtoolkit.plugins.packagemanagers.maven.tycho.addTychoExtension
import org.ossreviewtoolkit.utils.common.div

class MavenSupportTest : WordSpec({
    "isTychoProject()" should {
        "return true if the Tycho extension is found" {
            val projectDir = tempdir()
            projectDir.addTychoExtension()

            isTychoProject(projectDir) shouldBe true
        }

        "return false if the extension file does not contain the Tycho extension" {
            val projectDir = tempdir()
            projectDir.addTychoExtension(
                """
                <extensions>
                    <extension>
                        <groupId>org.eclipse.tycho</groupId>
                        <artifactId>tycho-foo</artifactId>
                        <version>4.0.0</version>
                    </extension>
                </extensions>
                """.trimIndent()
            )

            isTychoProject(projectDir) shouldBe false
        }

        "return false if there is no extension file" {
            val projectDir = tempdir()
            val mvnDir = projectDir / ".mvn"
            mvnDir.mkdirs()

            isTychoProject(projectDir) shouldBe false
        }

        "return false if there is no .mvn directory" {
            val projectDir = tempdir()

            isTychoProject(projectDir) shouldBe false
        }

        "return true for a pom file in a folder that has the Tycho extension" {
            val projectDir = tempdir()
            projectDir.addTychoExtension()

            val pomFile = projectDir / "pom.xml"

            isTychoProject(pomFile) shouldBe true
        }
    }
})
