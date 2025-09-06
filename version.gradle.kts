import java.io.BufferedReader
import java.io.InputStreamReader

private fun executeCommand(command: String): String {
    var ret = ""
    try {
        val process = ProcessBuilder(*command.split(" ").toTypedArray())
//            .redirectErrorStream(true)
            .start()
        val result = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.append(line).append("\n")
            }
        }
        process.waitFor()
        ret = result.toString().trim()
    } catch (ignored: Exception) {
    }
    if (ret.isEmpty()) {
        return "0"
    } else {
        return ret
    }
}

private val appMainVersion = 10000
private val appRevision = executeCommand("git describe --always")
private val commitCount = executeCommand("git rev-list --count HEAD")
private val date = executeCommand("date +'%Y%m%d'")
private val appVersionCode = appMainVersion * 10000 + commitCount.toInt()
private val appVersionName = "${appMainVersion / 10000}.${appMainVersion % 10000 / 100}.${appMainVersion % 100}.$appRevision"

extra["appVersionName"] = appVersionName
extra["appVersionCode"] = appVersionCode
