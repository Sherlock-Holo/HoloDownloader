package holoDownloader.cmdParser

class CmdParser {
    private val args = ArrayList<String>()
    private val parameters = HashMap<String, String>()

    var link: String? = null
        private set

    fun addParameter(arg: String) {
        args.add(arg)
    }

    fun parse(cmds: Array<String>) {
        val args = ArrayList<String>()
        args.addAll(cmds)
        val iter = args.iterator()

        while (iter.hasNext()) {
            val arg = iter.next()
            if (arg.startsWith("-") && arg in args) {
                iter.remove()
                parameters[arg] = iter.next()
                iter.remove()
            }
        }
        link = args[0]
    }

    fun getParameter(arg: String) = parameters[arg]
}