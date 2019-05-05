@file:Suppress("IMPLICIT_CAST_TO_ANY")

package com.adamratzman.golflin.links

import com.adamratzman.golflin.*
import com.adamratzman.golflin.GolflinNull.Companion.gNull

abstract class Transform(identifier: String, interpreter: GolflinVM) : Link(identifier, interpreter, diadic = true) {
    abstract fun evaluate(
        objects: List<GolflinObj>,
        transformer: List<GolflinObj>,
        register: MutableList<GolflinObj>
    ): List<GolflinObj>

    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        throw UnsupportedOperationException()
    }

    fun map(objects: List<GolflinObj>, transformer: List<GolflinObj>, register: MutableList<GolflinObj>) =
        objects.map { obj -> interpreter.evaluate(null, listOf(listOf(obj), transformer).flatten(), register) }
}

class GolflinBreak(interpreter: GolflinVM) : Link(";", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return argument!!
    }
}

class GolflinContextStart(interpreter: GolflinVM) : Link("{", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return argument!!
    }
}

class GolflinContextEnd(interpreter: GolflinVM) : Link("}", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return argument!!
    }
}

class GolflinContextVar(interpreter: GolflinVM) : Link("$", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return context
    }
}

class Range(interpreter: GolflinVM) : Link("R", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        val bound = (argument as GolflinNumber).number.toInt()
        return (1..bound).map { it.toGolflinObject() }.toGolflinObject()
    }
}

class ToNumber(interpreter: GolflinVM) : Link("N", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return when (argument) {
            is GolflinString -> argument.string.length
            is GolflinChar -> argument.char.toString().toIntOrNull() ?: 0
            is GolflinList -> argument.list.size
            is GolflinNumber -> argument.number
            else -> throw UnsupportedOperationException()
        }.toGolflinObject()
    }
}

class ToCharPoint(interpreter: GolflinVM) : Link("Ñ", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinChar
        return argument.char.toInt().toGolflinObject()
    }
}

class ToChar(interpreter: GolflinVM) : Link("ç", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return (argument as GolflinNumber).number.toChar().toGolflinObject()
    }
}

class Multiply(interpreter: GolflinVM) : Link("*", interpreter, diadic = true, strictDiadicIdentifier = "x") {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        val arg1 = argument[0]
        val arg2 = argument[1]
        return if (arg1 is GolflinNumber && arg2 is GolflinNumber) multiply(arg1, arg2)
        else if (arg1 is GolflinList && arg2 is GolflinNumber) arg1.map {
            multiply((it as GolflinNumber), arg2)
        }.toGolflinObject()
        else if (arg2 is GolflinList && arg1 is GolflinNumber) arg2.map {
            multiply((it as GolflinNumber), arg1)
        }.toGolflinObject()
        else throw IllegalArgumentException()
    }
}

class Divide(interpreter: GolflinVM) : Link("÷", interpreter, diadic = true, strictDiadicIdentifier = "/") {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        println("args $argument")
        val arg1 = argument[0]
        val arg2 = argument[1]
        return if (arg1 is GolflinNumber && arg2 is GolflinNumber) divide(arg1, arg2)
        else if (arg1 is GolflinList && arg2 is GolflinNumber) arg1.map {
            divide((it as GolflinNumber), arg2)
        }.toGolflinObject()
        else if (arg2 is GolflinList && arg1 is GolflinNumber) arg2.map {
            divide((it as GolflinNumber), arg1)
        }.toGolflinObject()
        else throw IllegalArgumentException("invalid: $argument")
    }
}

class Add(interpreter: GolflinVM) : Link("+", interpreter, diadic = true, strictDiadicIdentifier = "＋") {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        val arg1 = argument[0]
        val arg2 = argument[1]

        return if (arg1 is GolflinList) arg1.toMutableList().apply { add(arg2) }.toGolflinObject()
        else if (arg1 is GolflinChar && arg2 is GolflinChar) GolflinString("$arg1$arg2")
        else if (arg1 is GolflinString) GolflinString(arg1.string + arg2.toString())
        else if (arg1 is GolflinNumber && arg2 is GolflinNumber) (arg1.number + arg2.number).toGolflinObject()
        else throw IllegalArgumentException()
    }
}

class Subtract(interpreter: GolflinVM) : Link("-", interpreter, diadic = true, strictDiadicIdentifier = "`") {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        val arg1 = argument[0]
        val arg2 = argument[1]

        return when {
            arg1 is GolflinList -> arg1.toMutableList().apply { removeIf { it == arg2 } }.toGolflinObject()
            arg1 is GolflinString -> GolflinString(arg1.string.removeSuffix(arg2.toString()))
            arg1 is GolflinNumber && arg2 is GolflinNumber -> subtract(arg1, arg2)
            else -> throw IllegalArgumentException()
        }
    }
}

class Map(interpreter: GolflinVM) : Transform("M", interpreter) {
    override fun evaluate(
        objects: List<GolflinObj>,
        transformer: List<GolflinObj>,
        register: MutableList<GolflinObj>
    ): List<GolflinObj> {
        return map(objects, transformer, register)
    }
}

class Filter(interpreter: GolflinVM) : Transform("F", interpreter) {
    override fun evaluate(
        objects: List<GolflinObj>,
        transformer: List<GolflinObj>,
        register: MutableList<GolflinObj>
    ): List<GolflinObj> {
        return map(objects, transformer, register).mapIndexed { i, b -> (b as GolflinBoolean).boolean to objects[i] }
            .filter { it.first }
            .map { it.second }
    }
}

class FilterMapIndex(interpreter: GolflinVM) : Transform("y", interpreter) {
    override fun evaluate(
        objects: List<GolflinObj>,
        transformer: List<GolflinObj>,
        register: MutableList<GolflinObj>
    ): List<GolflinObj> {
        return map(objects, transformer, register).mapIndexed { i, b -> (b as GolflinBoolean).boolean to i }
            .filter { it.first }
            .map { it.second.toGolflinObject() }
    }
}


class Sum(interpreter: GolflinVM) : Link("S", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        return argument.sumByDouble { (it as? GolflinNumber)?.number ?: 0.0 }.toGolflinObject()
    }
}

class Retrieve(interpreter: GolflinVM) : Link("I", interpreter, diadic = true, alwaysStrictDiadic = true) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        return (argument[0] as GolflinList)[(argument[1] as GolflinNumber).number.toInt()]
    }
}

class Print(interpreter: GolflinVM) : Link("p", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        print(argument)
        return gNull
    }
}

class Println(interpreter: GolflinVM) : Link("P", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        println(argument)
        return gNull
    }
}

class Fibonacci(interpreter: GolflinVM) : Link("C", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        val nums = mutableListOf(1, 1)
        (2..(argument as GolflinNumber).number.toInt()).forEach { nums.add(nums[it - 1] + nums[it - 2]) }
        return nums.take(argument.number.toInt()).map { it.toGolflinObject() }.toGolflinObject()
    }
}

class Sublist(interpreter: GolflinVM) : Link("l", interpreter, diadic = true, alwaysStrictDiadic = true) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        val (list, bounds) = (argument as GolflinList).map { it as GolflinList }
        return list.subList((bounds[0] as GolflinNumber).number.toInt(), (bounds[1] as GolflinNumber).number.toInt())
            .toGolflinObject()
    }
}

class Equals(interpreter: GolflinVM) : Link("e", interpreter, diadic = true, strictDiadicIdentifier = "=") {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        return (argument[0].toObject() == argument[1].toObject()).toGolflinObject()
    }
}

class Not(interpreter: GolflinVM) : Link("!", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return (!(argument as GolflinBoolean).boolean).toGolflinObject()
    }
}

class If(interpreter: GolflinVM) : Link("?", interpreter, diadic = true) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        return argument[0].let {
            if ((it is GolflinNumber && it.number == 1.0) || (it is GolflinBoolean && it.boolean)) argument[1] else GolflinBreak(
                interpreter
            )
        }
    }
}

class False(interpreter: GolflinVM) : Link(":", interpreter, diadic = true) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        return argument[0].let {
            if (!(it is GolflinNumber && it.number == 1.0) && !(it is GolflinBoolean && it.boolean)) argument[1] else GolflinBreak(
                interpreter
            )
        }
    }
}


class GreaterThan(interpreter: GolflinVM) : Link("≥", interpreter, strictDiadicIdentifier = ">") {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        return ((argument[0] as GolflinNumber).number > (argument[1] as GolflinNumber).number).toGolflinObject()
    }
}

class LessThan(interpreter: GolflinVM) : Link("≤", interpreter, strictDiadicIdentifier = "<") {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        return ((argument[0] as GolflinNumber).number < (argument[1] as GolflinNumber).number).toGolflinObject()
    }
}

class IsInteger(interpreter: GolflinVM) : Link("&", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        val number = (argument as GolflinNumber).number
        return (number.toInt().toDouble() == number).toGolflinObject()
    }
}

class GetRegister(interpreter: GolflinVM) : Link("g", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return register.toGolflinObject()
    }
}

class DivideAndConquer(interpreter: GolflinVM) : Link("@", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return argument!!
    }
}

class GetRegister1(interpreter: GolflinVM) : Link("a", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return register[0]
    }
}

class GetRegister2(interpreter: GolflinVM) : Link("b", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return register[1]
    }
}

class GetRegister3(interpreter: GolflinVM) : Link("c", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return register[2]
    }
}

class GetRegister4(interpreter: GolflinVM) : Link("d", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return register[3]
    }
}

class GetRegister5(interpreter: GolflinVM) : Link("e", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        return register[4]
    }
}

class SaveToRegister(interpreter: GolflinVM) : Link("*", interpreter) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        register.add((argument as? GolflinList)?.last() ?: argument!!)
        return register.toGolflinObject()
    }
}

class Split(interpreter: GolflinVM) : Link("s", interpreter, diadic = true) {
    override fun evaluate(argument: GolflinObj?, context: GolflinObj, register: MutableList<GolflinObj>): GolflinObj {
        argument as GolflinList
        val toSplit = argument[0]
        val splitParam = argument[1]

        when (toSplit) {
            is GolflinString -> return toSplit.string.split((splitParam as GolflinString).string)
                .map { it.toGolflinObject() }.toGolflinObject()
            is GolflinList -> {
                val indices = toSplit.list.indices.filter { toSplit[it] == splitParam }
                return if (indices.isEmpty()) toSplit
                else GolflinList(indices.mapIndexed { index, i ->
                    toSplit.subList(indices.getOrNull(index - 1) ?: 0, i)
                        .toGolflinObject()
                }.toMutableList().apply {
                    if (indices.last() != toSplit.lastIndex) add(
                        toSplit.subList(toSplit.lastIndex, toSplit.size).toGolflinObject()
                    )
                }.filter { it !is GolflinList || it.isNotEmpty() })
            }
            else -> throw IllegalArgumentException()
        }
    }
}

fun multiply(a: GolflinNumber, b: GolflinNumber) = (a.number * b.number).toGolflinObject()
fun divide(a: GolflinNumber, b: GolflinNumber) = (a.number / b.number).toGolflinObject()
fun add(a: GolflinNumber, b: GolflinNumber) = (a.number + b.number).toGolflinObject()
fun subtract(a: GolflinNumber, b: GolflinNumber) = (a.number - b.number).toGolflinObject()