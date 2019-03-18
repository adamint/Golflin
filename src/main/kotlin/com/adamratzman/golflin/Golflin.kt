package com.adamratzman.golflin

import com.adamratzman.golflin.GolflinNull.Companion.gNull
import com.adamratzman.golflin.interpreter.Interpreter
import com.adamratzman.golflin.links.*
import com.adamratzman.golflin.links.Map

class GolflinVM(linkLambda: (GolflinVM) -> List<Link>) {
    val links: List<Link> = linkLambda(this)

    fun getProgramLinkLocations(program: String): List<GolflinObj> {
        println("Program: $program")
        var str = program.trim()
        val objects = mutableListOf<GolflinObj>()
        while (str.isNotBlank()) {
            if (str.startsWith("\"")) {
                val stringEnd = str.substring(1).withIndex()
                    .firstOrNull { (i, c) -> c == '"' && str[i] != '\\' }?.index?.plus(1) ?: str.lastIndex

                val actualString = str.substring(0, stringEnd + 1).replace("\\\"", "\"")
                objects.add(GolflinString(actualString.removeSuffix("\"").removePrefix("\"")))
                str = str.substring(stringEnd + 1)
            } else if (str.startsWith("[")) {
                val arrayEnd = str.substring(1).withIndex()
                    .firstOrNull { (i, c) -> c == ']' && str[i] != '\\' }?.index?.plus(1) ?: str.lastIndex

                objects.add(str.substring(1, arrayEnd).split("~").map {
                    Interpreter(
                        null,
                        getProgramLinkLocations(it),
                        mutableListOf(),
                        this
                    ).evaluateContexts()
                }.toGolflinObject())

                str = str.substring(arrayEnd + 1)
            } else {
                val foundLink = (0..str.lastIndex).mapNotNull { i ->
                    links.find { link ->
                        str.substring(i).startsWith(link.identifier) ||
                                link.strictDiadicIdentifier?.let { str.substring(i).startsWith(it) } == true
                    }?.to(i)
                }.firstOrNull()
                str = if (foundLink == null) {
                    objects.add(str.toGolflinObject())
                    ""
                } else {
                    val identifier =
                        if (!foundLink.first.alwaysStrictDiadic && str.substring(foundLink.second).startsWith(foundLink.first.identifier)) foundLink.first.identifier
                        else foundLink.first.strictDiadicIdentifier ?: foundLink.first.identifier

                    if (foundLink.second > 0) objects.add(str.substring(0, foundLink.second).toGolflinObject())
                    objects.add(foundLink.first)
                    if (identifier == foundLink.first.strictDiadicIdentifier) objects.add(StrictDiadicIndicator())
                    if (foundLink.second == str.lastIndex) ""
                    else str.substring(foundLink.second + identifier.length)
                }
            }
        }

        println("Objects: $objects")

        return objects
    }

    fun evaluate(input: GolflinObj?, links: List<GolflinObj>, register: MutableList<GolflinObj>): GolflinObj {
        return Interpreter(input,links,register=register,vm = this).evaluateContexts()
    }


    /*
    fun evaluate(input: String, program: String, register: MutableList<GolflinObj>): GolflinObj {
        // eventually, all lines except the last line will be transformed into links themselves, as functions
        return evaluate(
            if (input.isNotBlank()) evaluate(null, getProgramLinkLocations(input), register) else null,
            getProgramLinkLocations(program), register
        )
    }

    fun evaluate(input: GolflinObj?, program: String): GolflinObj =
        evaluate(
            input,
            getProgramLinkLocations(program),
            input?.let { if (it is GolflinList) it else mutableListOf(it) } ?: mutableListOf())

    fun evaluate(
        input: GolflinObj?,
        objects: List<GolflinObj>,
        register: MutableList<GolflinObj> = mutableListOf()
    ): GolflinObj {
        val objectsToReduce = objects.toMutableList()
        var value = input
            ?: objectsToReduce.removeAt(0)
        while (value is DivideAndConquer) {
            value = if (objectsToReduce.isNotEmpty()) objectsToReduce.removeAt(0) else GolflinReturnObj(0)
        }

        if (objectsToReduce.getOrNull(0) is DivideAndConquer) {
            value = objectsToReduce.getOrNull(1) ?: 1.toGolflinObject()
            objectsToReduce.removeAt(0)
            objectsToReduce.removeAt(0)
        }
        //if (value is Link) value.evaluate(input, register)

        while (objectsToReduce.isNotEmpty()) {
            println(objectsToReduce)
            val obj = objectsToReduce.first() as? Link ?: {
                if (value == input) {
                    value = objectsToReduce.removeAt(0)
                    objectsToReduce.firstOrNull() as? Link
                } else null
            }.invoke()
            if (obj == null) {
                value = objectsToReduce.firstOrNull() ?: value
                break
            }

            if (!obj.diadic) {
                value = obj.evaluate(value, register)
                objectsToReduce.removeAt(0)
            } else {
                if (obj !is Transform) {
                    if (!obj.alwaysStrictDiadic && objectsToReduce.getOrNull(1) !is StrictDiadicIndicator) {
                        value = obj.evaluate(
                            mutableListOf(
                                value,
                                evaluate(null, objectsToReduce.subList(1, objectsToReduce.size), register)
                            ).toGolflinObject(), register
                        )
                        objectsToReduce.clear()
                    } else {
                        value = obj.evaluate(
                            mutableListOf(
                                value,
                                if (obj.alwaysStrictDiadic) objectsToReduce[1] else objectsToReduce[2]
                            ).toGolflinObject(), register
                        )
                        if (!obj.alwaysStrictDiadic) objectsToReduce.removeAt(0)
                        objectsToReduce.removeAt(0)
                        objectsToReduce.removeAt(0)
                    }
                } else {
                    val breakIndex = objectsToReduce.subList(1, objectsToReduce.size)
                        .mapIndexedNotNull { i, g -> (g as? GolflinBreak)?.let { i } }.firstOrNull { index ->
                            objectsToReduce.subList(1, index)
                                .none { it is StrictDiadicIndicator || (it is Link && !it.alwaysStrictDiadic) }
                        }?.plus(1)
                        ?: objectsToReduce.lastIndex
                    val transformers = objectsToReduce.subList(1, breakIndex + 1)
                    println("Value: $value")
                    value = obj.evaluate(value as GolflinList, transformers, register).toGolflinObject()
                    if (breakIndex == objectsToReduce.lastIndex) objectsToReduce.clear()
                    else repeat((1..breakIndex).count()) { objectsToReduce.removeAt(0) }
                }
            }
        }
        return value
    }*/
}


val links: (GolflinVM) -> List<Link> = { i: GolflinVM ->
    listOf(
        Range(i),
        ToNumber(i),
        ToChar(i),
        Multiply(i),
        Add(i),
        GolflinBreak(i),
        GolflinContextStart(i),
        GolflinContextEnd(i),
        Map(i),
        Retrieve(i),
        ToCharPoint(i),
        Sum(i),
        Print(i),
        Println(i),
        Fibonacci(i),
        Sublist(i),
        Divide(i),
        Subtract(i),
        Equals(i),
        Not(i),
        GreaterThan(i),
        LessThan(i),
        IsInteger(i),
        Filter(i),
        GetRegister(i),
        DivideAndConquer(i),
        GetRegister1(i),
        GetRegister2(i),
        GetRegister3(i),
        GetRegister4(i),
        GetRegister5(i),
        SaveToRegister(i),
        False(i),
        GolflinContextVar(i),
        If(i),
        FilterMapIndex(i)
    )
}

fun main() {
    val vm = GolflinVM(links)
    while (true){
        val program = readLine()!!
        val links = vm.getProgramLinkLocations(program)
        val interpreter = Interpreter(null,links,vm = vm)
        interpreter.evaluateContexts().let { if (it != gNull) println(it) }
    }


    /*while (true) {
        print("Input Program: ")
        val input = readLine()!!
        val output = if (input.startsWith("?")) {
            val args = interpreter.evaluate("", input.substring(1), mutableListOf())
            readLine()!!.let {
                if (it.isNotEmpty()) interpreter.evaluate(
                    args.toGolflinObject(),
                    it
                ) else interpreter.evaluate(null, listOf(args))
            }
        } else interpreter.evaluate("", input, mutableListOf())
        println("Output: $output")
    }*/
}