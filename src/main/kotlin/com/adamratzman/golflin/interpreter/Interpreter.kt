package com.adamratzman.golflin.interpreter

import com.adamratzman.golflin.*
import com.adamratzman.golflin.links.GolflinBreak
import com.adamratzman.golflin.links.GolflinContextEnd
import com.adamratzman.golflin.links.GolflinContextStart
import com.adamratzman.golflin.links.Transform

class Interpreter(
    val input: GolflinObj?,
    objects: List<GolflinObj>,
    val register: MutableList<GolflinObj> = mutableListOf(),
    val vm: GolflinVM
) {
    val parsedContexts = getContexts(objects)


    fun getContexts(objects: List<GolflinObj>): GolflinList {
        // code blocks to execute inside each context
        // e.g. the program "M*2;+1" is mapped to [M*2, +1]
        // the program "S" ";MN{$siB?0:$X>10" is mapped to [S" ", MN, [$siB?0:$X]]

        val contexts = mutableListOf<GolflinObj>()
        var temp = objects.toMutableList()
        while (temp.indexOfFirst { it is GolflinContextStart } != -1) {
            val start = temp.indexOfFirst { it is GolflinContextStart }
            val end = temp.indexOfFirst { it is GolflinContextEnd }

            if (start != 0) contexts.add(getContexts(temp.subList(0, start)))

            if (end == -1) return GolflinList(contexts.apply {
                if (temp.size != 1) add(
                    getContexts(
                        temp.subList(
                            start + 1,
                            temp.size
                        )
                    )
                )
            })
            else {
                contexts.add(getContexts(temp.subList(start + 1, end)))
                temp = if (temp.size == 1) mutableListOf() else temp.subList(end + 1, temp.size)
            }
        }
        if (temp.isNotEmpty()) contexts.add(temp.toGolflinObject())

        return if (contexts.size == 1 && contexts[0] is GolflinList) contexts[0] as GolflinList else GolflinList(
            contexts
        )
    }

    fun evaluate(contextValue: GolflinObj, context: GolflinList): GolflinObj {
        println("Context: $context")
        val toRemove = context.split<GolflinBreak>().toMutableList()
        println("To Remove: $toRemove")
        if (toRemove.size > 1) {
            var value = evaluate(contextValue, toRemove[0])
            (1..toRemove.lastIndex).forEach { value = evaluate(value, toRemove[it]) }
            return value
        }

        val objects = toRemove.first().list

        var value = if (contextValue !is GolflinBreak) contextValue else objects.removeAt(0)
        if (value is Link) throw IllegalArgumentException("Value $value cannot be a link here")
        while (objects.isNotEmpty()) {
            val curr = objects.removeAt(0)
            if (curr !is Link) {
                value = curr
                continue
            }

            if (!curr.diadic) {
                value = curr.evaluate(value, contextValue, register)
                continue
            }

            if (curr is Transform) {
                return curr.evaluate(value as GolflinList, objects, register).toGolflinObject()
            }

            println("before: $objects")

            if (!curr.alwaysStrictDiadic && objects.getOrNull(0) !is StrictDiadicIndicator) {
                return curr.evaluate(
                    mutableListOf(
                        value,
                        evaluate(contextValue, GolflinList(objects))
                    ).toGolflinObject(), contextValue, register
                )
            } else {
                println(objects)
                value = curr.evaluate(
                    mutableListOf(
                        value,
                        if (curr.alwaysStrictDiadic) objects[0] else objects[1]
                    ).toGolflinObject(), contextValue, register
                )
                if (!curr.alwaysStrictDiadic) objects.removeAt(0)
                objects.removeAt(0)
            }
        }
        return value
    }

    fun evaluateContexts(): GolflinObj {
        return evaluate(input ?: GolflinBreak(vm),parsedContexts)
    }
}