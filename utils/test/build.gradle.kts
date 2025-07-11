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
}

dependencies {
    api(projects.model)
    api(projects.plugins.versionControlSystems.gitVersionControlSystem)

    api(libs.kotest.assertions.core)
    api(libs.kotest.framework.api)
    api(libs.logbackClassic) {
        because("Transitively export this to consumers so they do not have to declare a logger implementation.")
    }

    implementation(projects.downloader)
    implementation(projects.utils.ortUtils)

    implementation(libs.diffUtils)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jsonSchemaValidator)
    implementation(libs.kotest.extensions.junitXml)
    implementation(libs.kotest.framework.engine)
    implementation(libs.postgresEmbedded)

    runtimeOnly(libs.log4j.api.slf4j)
}
