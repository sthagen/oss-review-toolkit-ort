/*
 * Copyright (C) 2025 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.plugins.packagemanagers.maven.tycho

import org.apache.maven.project.MavenProject

import org.eclipse.aether.artifact.Artifact

import org.ossreviewtoolkit.model.Hash
import org.ossreviewtoolkit.model.Issue
import org.ossreviewtoolkit.model.RemoteArtifact

/**
 * A helper class that stores information about artifacts available in P2 repositories referenced by the current build.
 * When creating an instance using the [create] factory function, the class uses [P2RepositoryContentLoader] to load
 * the content information of the known P2 repositories. This information is then made available to Tycho, so that it
 * can create correct [RemoteArtifact] instances for the detected packages.
 */
internal class P2ArtifactResolver private constructor(
    /**
     * A list storing issues that occurred during the processing of the P2 repositories referenced by the current
     * build.
     */
    val resolverIssues: List<Issue>,

    /** A map that associates artifact keys with the URLs of the repositories they were downloaded from. */
    private val artifactRepositories: Map<String, String>,

    /** A map that associates artifact keys with their hashes. */
    private val artifactHashes: Map<String, Hash>
) {
    companion object {
        /** The ID of the layout for P2 repositories. */
        private const val P2_REPOSITORY_LAYOUT = "p2"

        /**
         * Create an instance of [P2ArtifactResolver] and initialize it with information from the given [targetHandler]
         * and the given [projects] that were found during the build.
         */
        fun create(targetHandler: TargetHandler, projects: Collection<MavenProject>): P2ArtifactResolver {
            val repositoryUrls = collectP2Repositories(targetHandler, projects)

            val (contents, issues) = P2RepositoryContentLoader.loadAllRepositoryContents(repositoryUrls)

            val artifactRepositories = mutableMapOf<String, String>()
            val artifactHashes = mutableMapOf<String, Hash>()
            contents.forEach { content ->
                content.artifacts.entries.forEach { (artifactId, hash) ->
                    artifactRepositories[artifactId] = content.baseUrl
                    artifactHashes[artifactId] = hash
                }
            }

            return P2ArtifactResolver(issues, artifactRepositories, artifactHashes)
        }

        /**
         * Find all P2 repositories defined for the current Tycho build based on the given [targetHandler]
         * and the list of encountered [projects]. This function is able to detect P2 repositories defined directly in
         * a project POM or in a Tycho target file.
         */
        internal fun collectP2Repositories(
            targetHandler: TargetHandler,
            projects: Collection<MavenProject>
        ): Set<String> = collectP2RepositoriesFromProjects(projects) + targetHandler.repositoryUrls

        /**
         * Collect all P2 repositories referenced from one of the given [projects].
         */
        private fun collectP2RepositoriesFromProjects(projects: Collection<MavenProject>): Set<String> =
            projects.flatMapTo(mutableSetOf()) { project ->
                project.remoteArtifactRepositories.filter { it.layout.id == P2_REPOSITORY_LAYOUT }.map { it.url }
            }

        /**
         * Generate the URL of the OSGi [artifact] in the repository with the given [repositoryUrl].
         */
        private fun artifactUrl(artifact: Artifact, repositoryUrl: String): String =
            "$repositoryUrl/plugins/${artifact.artifactId}_${artifact.version}.jar"

        /**
         * Return an [Artifact] to represent the source code bundle of the given [artifact].
         */
        private fun mapToSourceArtifact(artifact: Artifact): Artifact =
            object : Artifact by artifact {
                override fun getArtifactId(): String = "${artifact.artifactId}.source"
            }
    }

    /**
     * Construct a [RemoteArtifact] object for the binary of the given [artifact] that contains all information
     * available. If this artifact has been downloaded from a P2 repository, set the correct source URL. Also try to
     * obtain a checksum from the properties stored in the local Maven repository.
     */
    fun getBinaryArtifactFor(artifact: Artifact): RemoteArtifact {
        val key = "${artifact.artifactId}:${artifact.version}"

        return artifactRepositories[key]?.let { repositoryUrl ->
            RemoteArtifact(artifactUrl(artifact, repositoryUrl), artifactHashes.getValue(key))
        } ?: RemoteArtifact.EMPTY
    }

    /**
     * Construct a [RemoteArtifact] object for the source code bundle of the given [artifact] that contains all
     * information available. This function works analogously to [getSourceArtifactFor], but looks for an artifact
     * with the extension `.source`. This is the way how Tycho handles source code bundles.
     */
    fun getSourceArtifactFor(artifact: Artifact): RemoteArtifact = getBinaryArtifactFor(mapToSourceArtifact(artifact))
}
