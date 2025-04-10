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

package org.ossreviewtoolkit.plugins.packagemanagers.cocoapods

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.yamlList
import com.charleskorn.kaml.yamlMap
import com.charleskorn.kaml.yamlScalar

import org.ossreviewtoolkit.plugins.packagemanagers.cocoapods.Lockfile.CheckoutOption
import org.ossreviewtoolkit.plugins.packagemanagers.cocoapods.Lockfile.Dependency
import org.ossreviewtoolkit.plugins.packagemanagers.cocoapods.Lockfile.ExternalSource
import org.ossreviewtoolkit.plugins.packagemanagers.cocoapods.Lockfile.Pod

internal data class Lockfile(
    /** All pods that are transitively used in the project. */
    val pods: List<Pod>,

    /** The direct dependencies of the project. */
    val dependencies: List<Dependency>,

    /** Details about where external pods come from. */
    val externalSources: Map<String, ExternalSource>,

    /** Details about how to retrieve pods from external sources. */
    val checkoutOptions: Map<String, CheckoutOption>
) {
    private val podsByName by lazy { pods.associateBy { it.name } }

    inner class Pod(
        /** The name of this pod. */
        val name: String,

        /** The resolved version of this pod. */
        val version: String,

        /** The direct dependencies of this pod. */
        val dependencies: List<Dependency> = emptyList()
    ) {
        val externalSource = this@Lockfile.externalSources[name]
        val checkoutOption = this@Lockfile.checkoutOptions[name]
    }

    inner class Dependency(
        /** The name of this direct dependency. */
        val name: String,

        /** The version constraint for this direct dependency. */
        val versionConstraint: String?
    ) {
        val resolvedPod by lazy { this@Lockfile.podsByName[name] }
    }

    data class ExternalSource(
        /** The path to the local directory where the pod is hosted. */
        val path: String? = null,

        /** The path to the local '.podspec' file of the pod. */
        val podspec: String? = null
    )

    data class CheckoutOption(
        /** The Git repository URL to check out from. */
        val git: String?,

        /** The Git commit hash to check out. */
        val commit: String?,

        /** The Git tag name to check out. */
        val tag: String?,

        /** The Git branch name to check out. */
        val branch: String?
    )
}

internal fun String.parseLockfile(): Lockfile {
    val root = Yaml.default.parseToYamlNode(this).yamlMap

    val externalSources = root.get<YamlMap>("EXTERNAL SOURCES")?.entries.orEmpty().map {
        val name = it.key.content
        val node = it.value.yamlMap

        val externalSource = ExternalSource(
            path = node.get<YamlScalar>(":path")?.content,
            podspec = node.get<YamlScalar>(":podspec")?.content
        )

        name to externalSource
    }.toMap()

    val checkoutOptions = root.get<YamlMap>("CHECKOUT OPTIONS")?.entries.orEmpty().map {
        val name = it.key.content
        val node = it.value.yamlMap

        val checkoutOption = CheckoutOption(
            git = node.get<YamlScalar>(":git")?.content,
            commit = node.get<YamlScalar>(":commit")?.content,
            tag = node.get<YamlScalar>(":tag")?.content,
            branch = node.get<YamlScalar>(":branch")?.content
        )

        name to checkoutOption
    }.toMap()

    val pods = mutableListOf<Pod>()
    val dependencies = mutableListOf<Dependency>()

    val lockfile = Lockfile(pods, dependencies, externalSources, checkoutOptions)

    pods += root.get<YamlList>("PODS")?.items.orEmpty().map { it.toPod(lockfile) }
    dependencies += root.get<YamlList>("DEPENDENCIES")?.items.orEmpty().map { it.toDependency(lockfile) }

    return lockfile
}

private fun YamlNode.toPod(lockfile: Lockfile): Pod =
    when {
        this is YamlMap -> {
            val (key, value) = yamlMap.entries.entries.single()
            val (name, version) = parseNameAndVersion(key.content)
            val directDependencies = value.yamlList.items.map { it.toDependency(lockfile) }
            lockfile.Pod(name, checkNotNull(version), directDependencies)
        }

        else -> {
            val (name, version) = parseNameAndVersion(yamlScalar.content)
            lockfile.Pod(name, checkNotNull(version))
        }
    }

private fun YamlNode.toDependency(lockfile: Lockfile): Dependency {
    val (name, version) = parseNameAndVersion(yamlScalar.content)
    return lockfile.Dependency(name, version)
}

private fun parseNameAndVersion(entry: String): Pair<String, String?> {
    val info = entry.split(' ', limit = 2)
    val name = info[0]

    // A version entry could look something like "(6.3.0)", "(= 2021.06.28.00-v2)", "(~> 8.15.0)", etc. Also see
    // https://guides.cocoapods.org/syntax/podfile.html#pod.
    val version = info.getOrNull(1)?.removeSurrounding("(", ")")

    return name to version
}
