package com.adamratzman.golflin

abstract class Link(val identifier: String, val interpreter: Interpreter,
                    val diadic: Boolean = false, val strictDiadic: Boolean = false) : GolflinObj {
    abstract fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj
}

interface GolflinObj

class GolflinNumber(val number: Double) : GolflinObj {
    override fun toString() = if (number.toInt().toDouble() == number) number.toInt().toString() else number.toString()
}

class GolflinChar(val char: Char) : GolflinObj {
    override fun toString() = "'$char'"
}

class GolflinString(val string: String) : GolflinList(string.toList().map { GolflinChar(it) }.toGolflinObject() as GolflinList) {
    override fun toString() = "\"$string\""
}

open class GolflinList(val list: MutableList<GolflinObj> = mutableListOf()) : GolflinObj, ArrayList<GolflinObj>(list) {
    override fun toString() = list.toString()
}

class GolflinSequence<T>(val sequence: Sequence<T>) {
    override fun toString() = sequence.toString()
}

class GolflinReturnObj(val code: Int) : GolflinObj {
    override fun toString() = code.toString()
}

class GolflinBoolean(val boolean: Boolean):GolflinObj{
    override fun toString() = boolean.toString()
}

@Suppress("UNCHECKED_CAST")
fun Any.toGolflinObject() =
    if (toString().toDoubleOrNull() != null) GolflinNumber(this.toString().toDouble())
    else if (this == "f" || this == "t") GolflinBoolean(this == "t")
    else if (this is Char || (this is String && this.length == 1)) GolflinChar(this.toString()[0])
    else if (this is List<*>) GolflinList(this as MutableList<GolflinObj>)
    else if (this is String) GolflinString(this.removePrefix("\"").removeSuffix("\""))
    else if (this is Number) GolflinNumber(this.toDouble())
    else throw UnsupportedOperationException()