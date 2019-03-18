@file:Suppress("IMPLICIT_CAST_TO_ANY")

package com.adamratzman.golflin

import java.util.concurrent.CopyOnWriteArrayList

abstract class Link(
    val identifier: String, val interpreter: GolflinVM,
    val diadic: Boolean = false, val strictDiadicIdentifier: String? = null,
    val alwaysStrictDiadic: Boolean = false
) : GolflinObj {
    abstract fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj
}

interface GolflinObj

class GolflinNumber(val number: Double) : GolflinObj {
    override fun toString() = if (number.toInt().toDouble() == number) number.toInt().toString() else number.toString()
}

class GolflinChar(val char: Char) : GolflinObj {
    override fun toString() = "'$char'"
}

class GolflinString(val string: String) :
    GolflinList(string.toList().map { GolflinChar(it) }.toGolflinObject() as GolflinList) {
    override fun toString() = "\"$string\""
}

open class GolflinList(temp: MutableList<GolflinObj> = mutableListOf(),
                       val list: CopyOnWriteArrayList<GolflinObj> = CopyOnWriteArrayList(temp)
) :
    GolflinObj, ArrayList<GolflinObj>(list) {
    override fun toString() = list.toString()

    inline fun <reified T : GolflinObj> split(): List<GolflinList> {
        return list.indices.filter { list[it] is T }.let { indices ->
            if (indices.isEmpty()) listOf(this) else
                indices.map { index ->
                    list.subList(indices.getOrNull(index - 1) ?: 0, index)
                }.toMutableList().apply { if (indices.isNotEmpty()) add(list.subList(indices.last() + 1, list.size)) }
                    .filter { it.isNotEmpty() }.map { GolflinList(it) }
        }
    }
}

class GolflinSequence<T>(val sequence: Sequence<T>) : GolflinObj {
    override fun toString() = sequence.toString()
}

class GolflinReturnObj(val code: Int) : GolflinObj {
    override fun toString() = code.toString()
}

class GolflinBoolean(val boolean: Boolean) : GolflinObj {
    override fun toString() = boolean.toString()
}

class GolflinNull : GolflinObj {
    companion object{
        val gNull = GolflinNull()
    }

    override fun toString() = ""
}

class StrictDiadicIndicator : GolflinObj

@Suppress("UNCHECKED_CAST")
fun Any.toGolflinObject() =
    if (toString().toDoubleOrNull() != null) GolflinNumber(this.toString().toDouble())
    else if (this == "f" || this == "t") GolflinBoolean(this == "t")
    else if (this is Char || (this is String && this.length == 1)) GolflinChar(this.toString()[0])
    else if (this is List<*>) GolflinList(this as MutableList<GolflinObj>)
    else if (this is String) GolflinString(this.removePrefix("\"").removeSuffix("\""))
    else if (this is Number) GolflinNumber(this.toDouble())
    else if (this is Boolean) GolflinBoolean(this)
    else throw UnsupportedOperationException()

fun GolflinObj.toObject() = when (this) {
    is GolflinBoolean -> boolean
    is GolflinList -> list
    is GolflinNumber -> number
    is GolflinChar -> char
    is GolflinString -> string
    is GolflinReturnObj -> code
    is GolflinSequence<*> -> sequence
    else -> throw IllegalArgumentException()
}