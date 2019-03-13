@file:Suppress("IMPLICIT_CAST_TO_ANY")

package com.adamratzman.golflin.links

import com.adamratzman.golflin.*

abstract class Transform(identifier: String, interpreter: Interpreter) : Link(identifier, interpreter, diadic = true) {
    abstract fun evaluate(
        objects: List<GolflinObj>,
        transformer: List<GolflinObj>,
        register: List<GolflinObj>
    ): List<GolflinObj>

    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        throw UnsupportedOperationException()
    }

    fun map(objects: List<GolflinObj>, transformer: List<GolflinObj>) =
        objects.map { obj -> interpreter.evaluate(null, listOf(listOf(obj), transformer).flatten()) }
}

class Break(interpreter: Interpreter) : Link(";", interpreter) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        return argument!!
    }
}

class Range(interpreter: Interpreter) : Link("R", interpreter) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        val bound = (argument as GolflinNumber).number.toInt()
        return (1..bound).map { it.toGolflinObject() }.toGolflinObject()
    }
}

class ToNumber(interpreter: Interpreter) : Link("N", interpreter) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        return when (argument) {
            is GolflinString -> argument.string.length
            is GolflinChar -> argument.char.toString().toIntOrNull() ?: 0
            is GolflinList -> argument.list.size
            is GolflinNumber -> argument.number
            else -> throw UnsupportedOperationException()
        }.toGolflinObject()
    }
}

class ToCharPoint(interpreter: Interpreter) : Link("Ñ", interpreter) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        argument as GolflinChar
        return argument.char.toInt().toGolflinObject()
    }
}

class ToChar(interpreter: Interpreter) : Link("ç", interpreter) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        return (argument as GolflinNumber).number.toChar().toGolflinObject()
    }
}

class Multiply(interpreter: Interpreter) : Link("*", interpreter, diadic = true) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        argument as GolflinList
        val arg1 = argument[0]
        val arg2 = argument[1]
        return if (arg1 is GolflinNumber && arg2 is GolflinNumber) (arg1.number * arg2.number).toGolflinObject()
        else if (arg1 is GolflinList && arg2 is GolflinNumber) arg1.map { ((it as GolflinNumber).number * arg2.number).toGolflinObject() }.toGolflinObject()
        else if (arg2 is GolflinList && arg1 is GolflinNumber) arg2.map { ((it as GolflinNumber).number * arg1.number).toGolflinObject() }.toGolflinObject()
        else throw IllegalArgumentException()
    }
}


class Divide(interpreter: Interpreter) : Link("/", interpreter, diadic = true) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        argument as GolflinList
        val arg1 = argument[0]
        val arg2 = argument[1]
        return if (arg1 is GolflinNumber && arg2 is GolflinNumber) (arg1.number / arg2.number).toGolflinObject()
        else if (arg1 is GolflinList && arg2 is GolflinNumber) arg1.map { ((it as GolflinNumber).number / arg2.number).toGolflinObject() }.toGolflinObject()
        else if (arg2 is GolflinList && arg1 is GolflinNumber) arg2.map { ((it as GolflinNumber).number / arg1.number).toGolflinObject() }.toGolflinObject()
        else throw IllegalArgumentException()
    }
}

class Add(interpreter: Interpreter) : Link("+", interpreter, diadic = true) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
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

class Map(interpreter: Interpreter) : Transform("M", interpreter) {
    override fun evaluate(
        objects: List<GolflinObj>,
        transformer: List<GolflinObj>,
        register: List<GolflinObj>
    ): List<GolflinObj> {
        return map(objects, transformer)
    }
}

class Sum(interpreter: Interpreter) : Link("S", interpreter) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        argument as GolflinList
        return argument.sumByDouble { (it as? GolflinNumber)?.number ?: 0.0 }.toGolflinObject()
    }
}

class Retrieve(interpreter: Interpreter) : Link("I", interpreter, diadic = true, strictDiadic = true) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        argument as GolflinList
        return (argument[0] as GolflinList)[(argument[1] as GolflinNumber).number.toInt()]
    }
}

class Print(interpreter: Interpreter) : Link("p", interpreter) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        print(argument)
        return GolflinReturnObj(0)
    }
}

class Println(interpreter: Interpreter) : Link("P", interpreter) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        println(argument)
        return GolflinReturnObj(0)
    }
}

class Fibonacci(interpreter: Interpreter): Link("F", interpreter) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        val nums = mutableListOf(1,1)
        (2..(argument as GolflinNumber).number.toInt()).forEach { nums.add(nums[it - 1] + nums[it - 2]) }
        return nums.take(argument.number.toInt()).map { it.toGolflinObject() }.toGolflinObject()
    }
}

class Sublist(interpreter: Interpreter): Link("l", interpreter, diadic = true, strictDiadic = true) {
    override fun evaluate(argument: GolflinObj?, register: List<GolflinObj>): GolflinObj {
        val (list, bounds) = (argument as GolflinList).map { it as GolflinList }
       return list.subList((bounds[0] as GolflinNumber).number.toInt(), (bounds[1] as GolflinNumber).number.toInt()).toGolflinObject()
    }
}