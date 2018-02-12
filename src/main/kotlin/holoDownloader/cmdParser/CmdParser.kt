package holoDownloader.cmdParser

class CmdParser {
    private val args = ArrayList<String>()
    private val parameters = HashMap<String, String>()

    fun addParameter(arg: String) {
        args.add(arg)
    }

    fun parse(cmds: Array<String>) {
        cmds.forEach {
            if (args.contains(it)) {
                val index = cmds.indexOf(it)
                parameters[it] = cmds[index + 1]
            }
        }
    }

    fun getParameter(arg: String) = parameters[arg]
}