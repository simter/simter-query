package tech.simter.query.condition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.simter.query.condition.ConditionUtils.parse
import tech.simter.query.condition.support.*
import java.time.LocalDate

/**
 * Test [ConditionUtils.parse].
 *
 * @author RJ
 */
class ConditionUtilsTest {
  // '[[name, stringValue, valueType, symbol], ...]'
  private fun merge(vararg conditionStrings: String): String {
    return conditionStrings.joinToString(separator = ", ", prefix = "[", postfix = "]")
  }

  // =
  @Test
  fun `Parse EqualsCondition`() {
    // String value
    var c = EqualsCondition(name = "status", value = "Enabled")
    var s = parse(merge("""["status", "Enabled", "string", "="]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
    c = EqualsCondition(name = "status", value = "Enabled")
    s = parse(merge("""["status", "Enabled", "string"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
    c = EqualsCondition(name = "status", value = "Enabled")
    s = parse(merge("""["status", "Enabled"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])

    // LocalDate value
    c = EqualsCondition(name = "status", value = LocalDate.of(2018, 1, 20))
    s = parse(merge("""["status", "2018-01-20", "date", "="]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])

    // LocalDate value
    c = EqualsCondition(name = "status", value = LocalDate.of(2018, 1, 20))
    s = parse(merge("""["status", "2018-01-20", "date"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // >=
  @Test
  fun `Parse GreaterThanOrEqualsCondition`() {
    val c = GreaterThanOrEqualsCondition(name = "status", value = 9)
    val s = parse(merge("""["status", "9", "int", ">="]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // >
  @Test
  fun `Parse GreaterThanCondition`() {
    val c = GreaterThanCondition(name = "status", value = 9)
    val s = parse(merge("""["status", "9", "int", ">"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // <=
  @Test
  fun `Parse LessThanOrEqualsCondition`() {
    val c = LessThanOrEqualsCondition(name = "status", value = 9)
    val s = parse(merge("""["status", "9", "int", "<="]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // <
  @Test
  fun `Parse LessThanCondition`() {
    val c = LessThanCondition(name = "status", value = 9)
    val s = parse(merge("""["status", "9", "int", "<"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // in
  @Test
  fun `Parse InCondition`() {
    var c = InCondition(name = "status", value = listOf("a", "b"))
    var s = parse(merge("""["status", "[\"a\", \"b\"]", "string[]", "in"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])

    c = InCondition(name = "status", value = listOf(1, 2))
    s = parse(merge("""["status", "[1, 2]", "int[]", "in"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // is-null
  @Test
  fun `Parse IsNullCondition`() {
    val c = IsNullCondition(name = "status")
    val s = parse(merge("""["status", "", "", "is-null"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // between
  @Test
  fun `Parse BetweenCondition`() {
    val c = BetweenCondition(name = "status", value = Pair("a", "b"))
    val s = parse(merge("""["status", "[\"a\", \"b\"]", "string[]", "between"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // range []
  @Test
  fun `Parse RangeGteLteCondition - Both`() {
    val c = BetweenCondition(name = "status", value = Pair("a", "b"))
    val s = parse(merge("""["status", "[\"a\", \"b\"]", "string[]", "[]"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  @Test
  fun `Parse RangeGteLteCondition - Only first value`() {
    val c = GreaterThanOrEqualsCondition(name = "status", value = 9)
    var s = parse(merge("""["status", "[\"9\", \"\"]", "int[]", "[]"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])

    s = parse(merge("""["status", "[\"9\"]", "int[]", "[]"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  @Test
  fun `Parse RangeGteLteCondition - Only second value`() {
    val c = LessThanOrEqualsCondition(name = "status", value = 9)
    val s = parse(merge("""["status", "[\"\", \"9\"]", "int[]", "[]"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // range [)
  @Test
  fun `Parse RangeGteLtCondition - Both`() {
    val gte = GreaterThanOrEqualsCondition(name = "status", value = 1)
    val lt = LessThanCondition(name = "status", value = 2)
    val s = parse(merge("""["status", "[\"1\", \"2\"]", "int[]", "[)"]"""))
    assertEquals(2, s.size)
    assertEquals(gte, s[0])
    assertEquals(lt, s[1])
  }

  @Test
  fun `Parse RangeGteLtCondition - Only first value`() {
    val c = GreaterThanOrEqualsCondition(name = "status", value = 1)
    var s = parse(merge("""["status", "[\"1\", \"\"]", "int[]", "[)"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])

    s = parse(merge("""["status", "[\"1\"]", "int[]", "[)"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  @Test
  fun `Parse RangeGteLtCondition - Only second value`() {
    val c = LessThanCondition(name = "status", value = 1)
    val s = parse(merge("""["status", "[\"\", \"1\"]", "int[]", "[)"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // range (]
  @Test
  fun `Parse RangeGtLteCondition - Both`() {
    val gte = GreaterThanCondition(name = "status", value = 1)
    val lt = LessThanOrEqualsCondition(name = "status", value = 2)
    val s = parse(merge("""["status", "[\"1\", \"2\"]", "int[]", "(]"]"""))
    assertEquals(2, s.size)
    assertEquals(gte, s[0])
    assertEquals(lt, s[1])
  }

  @Test
  fun `Parse RangeGtLteCondition - Only first value`() {
    val c = GreaterThanCondition(name = "status", value = 1)
    var s = parse(merge("""["status", "[\"1\", \"\"]", "int[]", "(]"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])

    s = parse(merge("""["status", "[\"1\"]", "int[]", "(]"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  @Test
  fun `Parse RangeGtLteCondition - Only second value`() {
    val c = LessThanOrEqualsCondition(name = "status", value = 1)
    val s = parse(merge("""["status", "[\"\", \"1\"]", "int[]", "(]"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  // range ()
  @Test
  fun `Parse RangeGtLtCondition - Both`() {
    val gte = GreaterThanCondition(name = "status", value = 1)
    val lt = LessThanCondition(name = "status", value = 2)
    val s = parse(merge("""["status", "[\"1\", \"2\"]", "int[]", "()"]"""))
    assertEquals(2, s.size)
    assertEquals(gte, s[0])
    assertEquals(lt, s[1])
  }

  @Test
  fun `Parse RangeGtLtCondition - Only first value`() {
    val c = GreaterThanCondition(name = "status", value = 1)
    var s = parse(merge("""["status", "[\"1\", \"\"]", "int[]", "()"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])

    s = parse(merge("""["status", "[\"1\"]", "int[]", "()"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

  @Test
  fun `Parse RangeGtLtCondition - Only second value`() {
    val c = LessThanCondition(name = "status", value = 1)
    val s = parse(merge("""["status", "[\"\", \"1\"]", "int[]", "()"]"""))
    assertEquals(1, s.size)
    assertEquals(c, s[0])
  }

@Test
fun example() {
  val conditionsString = """[
    ["k", "2018-01-10", "date", "="],
    ["k", "[3]", "int[]", "()"],
    ["k", "[6, 9]", "int[]", "()"]
  ]""".trimIndent()
  val conditions: List<Condition<*>> = ConditionUtils.parse(conditionsString)

  assertEquals(4, conditions.size)
  assertEquals(EqualsCondition(name = "k", value = LocalDate.of(2018, 1, 10)), conditions[0])
  assertEquals(GreaterThanCondition(name = "k", value = 3), conditions[1])
  assertEquals(GreaterThanCondition(name = "k", value = 6), conditions[2])
  assertEquals(LessThanCondition(name = "k", value = 9), conditions[3])
}
}