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

plugins {
    // Apply precompiled plugins.
    id("ort-library-conventions")

    // Apply third-party plugins.
    alias(libs.plugins.buildConfig)
}

buildConfig {
    packageName = "$group.${projectDir.name}"

    buildConfigField("ORT_VERSION", provider { project.version.toString() })
}

dependencies {
    api(projects.utils.commonUtils)
    api(projects.utils.spdxUtils)

    api(libs.kotlinx.coroutines)
    api(libs.okhttp)

    implementation(projects.clients.foojay)

    implementation(libs.awsS3)
    implementation(libs.commonsCompress)
    implementation(libs.xz)

    testImplementation(libs.mockk)
}
