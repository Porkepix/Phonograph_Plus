/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph

import util.phonograph.changelog.writeToFile
import util.phonograph.changelog.generateGitHubReleaseMarkDown
import util.phonograph.changelog.parseReleaseNote

fun main(args: Array<String>) {

    val rootPath = args[0]
    val sourcePath = args[1]
    val targetPath = args[2]

    val model = parseReleaseNote("$rootPath/$sourcePath")

    val markdown = generateGitHubReleaseMarkDown(model)
    writeToFile(markdown, "$rootPath/$targetPath")

    println(markdown)
}