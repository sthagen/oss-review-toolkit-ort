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

package org.ossreviewtoolkit.plugins.scanners.scancode

import io.kotest.engine.spec.tempdir
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.endWith
import io.kotest.matchers.string.startWith

import org.ossreviewtoolkit.model.LicenseFinding
import org.ossreviewtoolkit.model.TextLocation
import org.ossreviewtoolkit.scanner.AbstractPathScannerWrapperFunTest
import org.ossreviewtoolkit.utils.spdx.getLicenseText

class ScanCodeScannerFunTest : AbstractPathScannerWrapperFunTest() {
    override val scanner = ScanCodeFactory.create()

    override val expectedFileLicenses = listOf(
        LicenseFinding("Apache-2.0", TextLocation("LICENSE", 1, 201), 100.0f)
    )

    override val expectedDirectoryLicenses = listOf(
        LicenseFinding("Apache-2.0", TextLocation("COPYING", 1, 201), 99.43f),
        LicenseFinding("Apache-2.0", TextLocation("LICENCE", 1, 201), 100.0f),
        LicenseFinding("Apache-2.0", TextLocation("LICENSE", 1, 201), 100.0f)
    )

    init {
        "return the full license text for the HERE proprietary license" {
            val text = getLicenseText("LicenseRef-scancode-here-proprietary")?.trim()

            text should startWith("This software and other materials contain proprietary information")
            text should endWith("allowed.")
        }

        "return the full license text for a known SPDX LicenseRef" {
            val text = getLicenseText("LicenseRef-scancode-indiana-extreme")?.trim()

            text should startWith("Indiana University Extreme! Lab Software License Version 1.1.1")
            text should endWith("EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.")
        }

        "return the ort license text for a license ID known by ort and also in custom dir" {
            val id = "LicenseRef-scancode-here-proprietary"
            val text = "x\ny\n"

            val outputDir = tempdir().apply { resolve(id).writeText(text) }

            getLicenseText(id, true, listOf(outputDir)) shouldBe getLicenseText(id, true)
        }
    }
}
