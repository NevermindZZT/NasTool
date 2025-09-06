package com.letter.nastool.utils.ext

import java.io.File

fun String.joinPath(other: String): String {
    var thisTrimmed = this
    if (thisTrimmed.endsWith(File.separator)) {
        thisTrimmed = thisTrimmed.dropLast(File.separator.length)
    }
    var otherTrimmed = other
    if (otherTrimmed.startsWith(File.separator)) {
        otherTrimmed = otherTrimmed.drop(File.separator.length)
    }
    return "$thisTrimmed${File.separator}$otherTrimmed"
}