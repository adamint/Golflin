package com.adamratzman.golflin

import com.adamratzman.golflin.links.*
import com.adamratzman.golflin.links.Map

class Interpreter(linkLambda: (Interpreter) -> List<Link>) {
    val links: List<Link> = linkLambda(this)

    fun evaluate(input: String, program: String): GolflinObj {
        // eventually, all lines except the last line will be transformed into links themselves, as functions
        return evaluate(
            if (input.isNotBlank()) evaluate(null, getProgramLinkLocations(input)) else null,
            getProgramLinkLocations(program)
        )
    }

    fun evaluate(input: GolflinObj?, program: String): GolflinObj =
        evaluate(input, getProgramLinkLocations(program))

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

                objects.add(str.substring(1,arrayEnd).split("~").map { evaluate(null, getProgramLinkLocations(it)) }.toGolflinObject())

                str = str.substring(arrayEnd + 1)
            }
            else {
                val foundLink = (0..str.lastIndex).mapNotNull { i ->
                    links.find { str.substring(i).startsWith(it.identifier) }?.to(i)
                }.firstOrNull()
                str = if (foundLink == null) {
                    objects.add(str.toGolflinObject())
                    ""
                } else {
                    if (foundLink.second > 0) objects.add(str.substring(0, foundLink.second).toGolflinObject())
                    objects.add(foundLink.first)
                    if (foundLink.second == str.lastIndex) ""
                    else str.substring(foundLink.second + foundLink.first.identifier.length)
                }
            }
        }

        println("Objects: $objects")

        return objects
    }

    fun evaluate(
        input: GolflinObj?,
        objects: List<GolflinObj>,
        register: MutableList<GolflinObj> = mutableListOf()
    ): GolflinObj {
        val objectsToReduce = objects.toMutableList()
        var value = input
            ?: objectsToReduce.removeAt(0)
        if (value is Link) value.evaluate(input, register)

        while (objectsToReduce.isNotEmpty()) {
            val obj = objectsToReduce.first() as? Link
                ?: throw IllegalArgumentException("${objectsToReduce.first()} must be a Link")
            if (!obj.diadic) {
                value = obj.evaluate(value, register)
                objectsToReduce.removeAt(0)
            } else {
                if (obj !is Transform) {
                    if (!obj.strictDiadic) {
                        value = obj.evaluate(
                            mutableListOf(
                                value,
                                evaluate(null, objectsToReduce.subList(1, objectsToReduce.size))
                            ).toGolflinObject(), register
                        )
                        objectsToReduce.clear()
                    } else {
                        value = obj.evaluate(
                            mutableListOf(
                                value,
                                objectsToReduce[1]
                            ).toGolflinObject(), register
                        )
                        objectsToReduce.removeAt(0)
                        objectsToReduce.removeAt(0)
                    }
                } else {
                    val breakIndex = objectsToReduce.subList(1, objectsToReduce.size)
                        .mapIndexedNotNull { i, g -> (g as? Break)?.let { i } }.firstOrNull()?.plus(1)
                        ?: objectsToReduce.lastIndex
                    value as GolflinList
                    val transformers = objectsToReduce.subList(1, breakIndex + 1)
                    value = obj.evaluate(value, transformers, register).toGolflinObject()
                    if (breakIndex == objectsToReduce.lastIndex) objectsToReduce.clear()
                    else repeat((1..breakIndex).count()) { objectsToReduce.removeAt(0) }
                }
            }
        }
        return value
    }
}


val links: (Interpreter) -> List<Link> = { i: Interpreter ->
    listOf(
        Range(i),
        ToNumber(i),
        ToChar(i),
        Multiply(i),
        Add(i),
        Break(i),
        Map(i),
        Retrieve(i),
        ToCharPoint(i),
        Sum(i),
        Print(i),
        Println(i),
        Fibonacci(i),
        Sublist(i),
        Divide(i)
    )
}

fun main() {
    val interpreter = Interpreter(links)
    while (true) {
        print("Input Program: ")
        val input = readLine()!!
        val output = if (input.startsWith("?")) {
            val args = interpreter.evaluate("", input.substring(1))
            readLine()!!.let {
                if (it.isNotEmpty()) interpreter.evaluate(
                    args.toGolflinObject(),
                    it
                ) else interpreter.evaluate(null, listOf(args))
            }
        } else interpreter.evaluate("", input)

        println("Output: $output")
    }
}