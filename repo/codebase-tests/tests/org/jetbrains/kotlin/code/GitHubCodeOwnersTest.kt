/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import junit.framework.TestCase
import java.io.File
import java.nio.file.Files.createTempDirectory

class GitHubOwnersTest : TestCase() {

    fun testGitHubCodeOwnersWasGeneratedCorrectly() {
        // Get file paths from system properties (set by CommandLineArgumentProvider in build.gradle.kts)
        val scriptFilePath = System.getProperty("codeOwnersTest.scriptFile")
        val spaceCodeOwnersFilePath = System.getProperty("codeOwnersTest.spaceCodeOwnersFile")
        val virtualTeamMappingFilePath = System.getProperty("codeOwnersTest.virtualTeamMappingFile")
        val githubCodeOwnersFilePath = System.getProperty("codeOwnersTest.githubCodeOwnersFile")

        // Create File objects from the paths
        val scriptFile = File(scriptFilePath)
        assertTrue("Script file does not exist: ${scriptFile.absolutePath}", scriptFile.exists())

        // Create a temporary directory to store the output
        val tempDir = createTempDirectory("github-codeowners-test").toFile()
        tempDir.deleteOnExit()

        // Create a copy of the .space directory structure in the temp directory
        val tempSpaceDir = File(tempDir, ".space")
        tempSpaceDir.mkdirs()

        // Copy the necessary files to the temp directory
        File(spaceCodeOwnersFilePath).copyTo(File(tempSpaceDir, "CODEOWNERS"))
        File(virtualTeamMappingFilePath).copyTo(File(tempSpaceDir, "virtual-team-mapping.json"))
        scriptFile.copyTo(File(tempSpaceDir, "generate-github-codeowners.sh"))

        // Create .github directory in the temp directory
        val tempGithubDir = File(tempDir, ".github")
        tempGithubDir.mkdirs()

        try {
            // Run the script in the temp directory
            val process = ProcessBuilder("bash", File(tempSpaceDir, "generate-github-codeowners.sh").absolutePath)
                .directory(tempDir)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            val exitCode = process.waitFor()
            assertEquals("Script execution failed with exit code: $exitCode", 0, exitCode)

            // Verify that the script created the CODEOWNERS file in the .github directory
            val generatedFile = File(tempGithubDir, "CODEOWNERS")
            assertTrue("Script did not generate the CODEOWNERS file", generatedFile.exists())

            val originalFile = File(githubCodeOwnersFilePath)
            assertTrue(
                "Generated GitHub CODEOWNERS does not match the actual file, did you forget to run .space/generate-github-codeowners.sh?",
                generatedFile.readText() == originalFile.readText()
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
