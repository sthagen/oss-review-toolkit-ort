/*
 * Copyright (C) 2020 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.plugins.reporters.spdx

import org.ossreviewtoolkit.model.AnalyzerResult
import org.ossreviewtoolkit.model.AnalyzerRun
import org.ossreviewtoolkit.model.ArtifactProvenance
import org.ossreviewtoolkit.model.CopyrightFinding
import org.ossreviewtoolkit.model.Hash
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.LicenseFinding
import org.ossreviewtoolkit.model.OrtResult
import org.ossreviewtoolkit.model.Package
import org.ossreviewtoolkit.model.PackageReference
import org.ossreviewtoolkit.model.Project
import org.ossreviewtoolkit.model.RemoteArtifact
import org.ossreviewtoolkit.model.Repository
import org.ossreviewtoolkit.model.RepositoryProvenance
import org.ossreviewtoolkit.model.ScanResult
import org.ossreviewtoolkit.model.ScanSummary
import org.ossreviewtoolkit.model.ScannerDetails
import org.ossreviewtoolkit.model.Scope
import org.ossreviewtoolkit.model.TextLocation
import org.ossreviewtoolkit.model.VcsInfo
import org.ossreviewtoolkit.model.VcsType
import org.ossreviewtoolkit.model.config.Excludes
import org.ossreviewtoolkit.model.config.RepositoryConfiguration
import org.ossreviewtoolkit.model.config.ScopeExclude
import org.ossreviewtoolkit.model.config.ScopeExcludeReason
import org.ossreviewtoolkit.utils.spdx.toSpdx
import org.ossreviewtoolkit.utils.test.scannerRunOf

private val ANALYZED_VCS = VcsInfo(
    type = VcsType.GIT,
    revision = "master",
    url = "https://github.com/path/first-project.git"
)

val ORT_RESULT = OrtResult(
    repository = Repository(
        config = RepositoryConfiguration(
            excludes = Excludes(
                scopes = listOf(
                    ScopeExclude(
                        pattern = "test",
                        reason = ScopeExcludeReason.TEST_DEPENDENCY_OF,
                        comment = "Packages for testing only."
                    )
                )
            )
        ),
        vcs = ANALYZED_VCS,
        vcsProcessed = ANALYZED_VCS
    ),
    analyzer = AnalyzerRun.EMPTY.copy(
        result = AnalyzerResult(
            projects = setOf(
                Project(
                    id = Identifier("Maven:first-project-group:first-project-name:0.0.1"),
                    declaredLicenses = setOf("MIT"),
                    definitionFilePath = "",
                    homepageUrl = "https://example.com/first-project/homepage",
                    scopeDependencies = setOf(
                        Scope(
                            name = "compile",
                            dependencies = setOf(
                                PackageReference(
                                    id = Identifier("Maven:first-package-group:first-package:0.0.1"),
                                    dependencies = setOf(
                                        PackageReference(
                                            id = Identifier("Maven:second-package-group:second-package:0.0.1")
                                        ),
                                        PackageReference(
                                            id = Identifier("Maven:third-package-group:third-package:0.0.1"),
                                            dependencies = setOf(
                                                PackageReference(
                                                    id = Identifier("Maven:sixth-package-group:sixth-package:0.0.1")
                                                )
                                            )
                                        )
                                    )
                                ),
                                PackageReference(
                                    id = Identifier("Maven:fourth-package-group:fourth-package:0.0.1"),
                                    dependencies = setOf(
                                        PackageReference(
                                            id = Identifier("Maven:seventh-package-group:seventh-package:0.0.1")
                                        )
                                    )
                                )
                            )
                        ),
                        Scope(
                            name = "test",
                            dependencies = setOf(
                                PackageReference(
                                    id = Identifier("Maven:fifth-package-group:fifth-package:0.0.1")
                                )
                            )
                        )
                    ),
                    vcs = ANALYZED_VCS
                )
            ),
            packages = setOf(
                Package(
                    id = Identifier("Maven:first-package-group:first-package:0.0.1"),
                    binaryArtifact = RemoteArtifact(
                        url = "https://example.com/first-package.jar",
                        hash = Hash.create("0000000000000000000000000000000000000000")
                    ),
                    concludedLicense = "BSD-2-Clause AND BSD-3-Clause AND MIT".toSpdx(),
                    declaredLicenses = setOf("BSD-3-Clause", "MIT OR GPL-2.0-only"),
                    description = "A package with all supported attributes set, with a VCS URL containing a user " +
                        "name, and with two scan results for the VCS containing copyright findings matched to a " +
                        "license finding.",
                    homepageUrl = "https://example.com/first-package/homepage",
                    sourceArtifact = RemoteArtifact(
                        url = "https://example.com/first-package-sources.jar",
                        hash = Hash.create("0000000000000000000000000000000000000000")
                    ),
                    vcs = VcsInfo(
                        type = VcsType.GIT,
                        revision = "master",
                        url = "ssh://git@github.com/path/first-package-repo.git",
                        path = "project-path"
                    )
                ),
                Package(
                    id = Identifier("Maven:second-package-group:second-package:0.0.1"),
                    binaryArtifact = RemoteArtifact.EMPTY,
                    declaredLicenses = emptySet(),
                    description = "A package with minimal attributes set.",
                    homepageUrl = "",
                    sourceArtifact = RemoteArtifact.EMPTY,
                    vcs = VcsInfo.EMPTY
                ),
                Package(
                    id = Identifier("Maven:third-package-group:third-package:0.0.1"),
                    binaryArtifact = RemoteArtifact.EMPTY,
                    declaredLicenses = setOf("unmappable license"),
                    description = "A package with only unmapped declared license.",
                    homepageUrl = "",
                    sourceArtifact = RemoteArtifact.EMPTY,
                    vcs = VcsInfo.EMPTY
                ),
                Package(
                    id = Identifier("Maven:fourth-package-group:fourth-package:0.0.1"),
                    binaryArtifact = RemoteArtifact.EMPTY,
                    declaredLicenses = setOf("unmappable license", "MIT"),
                    description = "A package with partially mapped declared license.",
                    homepageUrl = "",
                    sourceArtifact = RemoteArtifact.EMPTY,
                    vcs = VcsInfo.EMPTY
                ),
                Package(
                    id = Identifier("Maven:fifth-package-group:fifth-package:0.0.1"),
                    binaryArtifact = RemoteArtifact.EMPTY,
                    declaredLicenses = setOf("LicenseRef-scancode-philips-proprietary-notice-2000"),
                    concludedLicense = "LicenseRef-scancode-purdue-bsd".toSpdx(),
                    description = "A package used only from the excluded 'test' scope, with non-SPDX license IDs in " +
                        "the declared and concluded license.",
                    homepageUrl = "",
                    sourceArtifact = RemoteArtifact.EMPTY,
                    vcs = VcsInfo.EMPTY
                ),
                Package(
                    id = Identifier("Maven:sixth-package-group:sixth-package:0.0.1"),
                    binaryArtifact = RemoteArtifact.EMPTY,
                    declaredLicenses = setOf("LicenseRef-scancode-asmus"),
                    concludedLicense = "LicenseRef-scancode-srgb".toSpdx(),
                    description = "A package with non-SPDX license IDs in the declared and concluded license.",
                    homepageUrl = "",
                    sourceArtifact = RemoteArtifact.EMPTY,
                    vcs = VcsInfo.EMPTY
                ),
                Package(
                    id = Identifier("Maven:seventh-package-group:seventh-package:0.0.1"),
                    binaryArtifact = RemoteArtifact.EMPTY,
                    declaredLicenses = setOf(),
                    description = "A package with a source artifact scan result.",
                    homepageUrl = "",
                    sourceArtifact = RemoteArtifact(
                        url = "https://example.com/seventh-package-sources.jar",
                        hash = Hash.create("0000000000000000000000000000000000000000")
                    ),
                    vcs = VcsInfo.EMPTY
                )
            )
        )
    ),
    scanner = scannerRunOf(
        Identifier("Maven:first-package-group:first-package:0.0.1") to listOf(
            ScanResult(
                provenance = RepositoryProvenance(
                    vcsInfo = VcsInfo(
                        type = VcsType.GIT,
                        revision = "master",
                        url = "ssh://git@github.com/path/first-package-repo.git",
                        path = "project-path"
                    ),
                    resolvedRevision = "deadbeef"
                ),
                scanner = ScannerDetails.EMPTY.copy(name = "scanner1"),
                summary = ScanSummary.EMPTY.copy(
                    licenseFindings = setOf(
                        LicenseFinding(
                            license = "Apache-2.0",
                            location = TextLocation("LICENSE", 1)
                        )
                    ),
                    copyrightFindings = setOf(
                        CopyrightFinding(
                            statement = "Copyright 2020 Some copyright holder in source artifact",
                            location = TextLocation("project-path/some/file", 1)
                        ),
                        CopyrightFinding(
                            statement = "Copyright 2020 Some other copyright holder in source artifact",
                            location = TextLocation("project-path/some/file", 7)
                        )
                    )
                )
            ),
            ScanResult(
                provenance = RepositoryProvenance(
                    vcsInfo = VcsInfo(
                        type = VcsType.GIT,
                        revision = "master",
                        url = "ssh://git@github.com/path/first-package-repo.git",
                        path = "project-path"
                    ),
                    resolvedRevision = "deadbeef"
                ),
                scanner = ScannerDetails.EMPTY.copy(name = "scanner2"),
                summary = ScanSummary.EMPTY.copy(
                    licenseFindings = setOf(
                        LicenseFinding(
                            license = "BSD-2-Clause",
                            location = TextLocation("LICENSE", 1)
                        )
                    ),
                    copyrightFindings = setOf(
                        CopyrightFinding(
                            statement = "Copyright 2020 Some copyright holder in VCS",
                            location = TextLocation("project-path/some/file", 1)
                        )
                    )
                )
            )
        ),
        Identifier("Maven:seventh-package-group:seventh-package:0.0.1") to listOf(
            ScanResult(
                provenance = ArtifactProvenance(
                    sourceArtifact = RemoteArtifact(
                        url = "https://example.com/seventh-package-sources.jar",
                        hash = Hash.create("0000000000000000000000000000000000000000")
                    )
                ),
                scanner = ScannerDetails.EMPTY,
                summary = ScanSummary.EMPTY.copy(
                    licenseFindings = setOf(
                        LicenseFinding(
                            license = "GPL-2.0-only WITH NOASSERTION",
                            location = TextLocation("LICENSE", 1)
                        )
                    ),
                    copyrightFindings = setOf(
                        CopyrightFinding(
                            statement = "Copyright 2020 Some copyright holder in source artifact",
                            location = TextLocation("some/file", 1)
                        )
                    )
                )
            )
        )
    )
)
