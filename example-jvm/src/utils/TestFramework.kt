package utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KFunction

data class Result(
        val obj: Any, // original object
        val res: Any, // resulting object
        val ext: Any  // serialized (external) representation
)

class Case<T: Any>(
        val obj: T,
        val name: String = obj.javaClass.simpleName,
        val hasNulls: Boolean = false
) {
    @Suppress("UNCHECKED_CAST")
    val serializer: KSerializer<T> = obj::class.serializer() as KSerializer<T>
}

val testCases: List<Case<*>> = listOf(
        Case(CityData(1, "New York")),
        Case(StreetData(2, "Broadway", CityData(1, "New York"))),
        Case(StreetData2(2, "Broadway", CityData(1, "New York"))),
        Case(StreetData2(2, "Broadway", null), hasNulls = true),
        Case(CountyData("US", listOf(CityData(1, "New York"), CityData(2, "Chicago")))),
        Case(zoo, hasNulls = true),
        Case(shop)
)

@Suppress("UNCHECKED_CAST")
fun <T: Any> testCase(serializer: KSerializer<T>, obj: T, method: (KSerializer<Any>, Any) -> Result, verbose: Boolean = true): Boolean {
    if (verbose) println("Start with $obj")
    val result = try { method(serializer as KSerializer<Any>, obj) } catch (e: Throwable) {
        println("Failed with $e")
        return false
    }
    if (verbose) {
        println("Loaded obj ${result.res}")
        println("    equals=${obj == result.res}, sameRef=${obj === result.res}")
        println("Saved form ${result.ext}")
    }
    return obj == result.res
}

fun testCase(case: Case<Any>, method: (KSerializer<Any>, Any) -> Result, verbose: Boolean = true): Boolean {
    println("Test case ${case.name}")
    return testCase(case.serializer, case.obj, method, verbose)
}

@Suppress("UNCHECKED_CAST")
fun testMethod(method: (KSerializer<Any>, Any) -> Result, supportsNull: Boolean = true, verbose: Boolean = true): Pair<Int, Int> {
    if (verbose) println("==============================================")
    println("Running with ${(method as KFunction<*>).name}")
    var totalCount = 0
    var failCount = 0
    testCases.forEach { case ->
        if (!supportsNull && case.hasNulls) return@forEach
        if (verbose) println()
        if (!testCase(case as Case<Any>, method, verbose))
            failCount++
        totalCount++
    }
    if (verbose) {
        println("==============================================")
        println("Done with ${method.name}")
        if (failCount > 0)
            println("!!! FAILED $failCount TEST CASES OUT OF $totalCount TEST CASES !!!")
        else
            println("Passed $totalCount test cases")
    }
    if (failCount > 0) throw Exception("Not all tests passed!")
    return Pair(failCount, totalCount)
}