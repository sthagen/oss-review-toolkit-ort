/*
 * Copyright (C) 2017-2024 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.plugins.packagemanagers.stack

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue

import org.ossreviewtoolkit.model.jsonMapper

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Location(
    val url: String,
    val type: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Dependency(
    val name: String,
    val version: String,
    val license: String,
    val location: Location? = null,
    val dependencies: List<String> = emptyList()
)

internal fun String.parseDependencies() = jsonMapper.readValue<List<Dependency>>(this)
