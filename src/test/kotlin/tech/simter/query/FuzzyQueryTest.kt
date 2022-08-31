package tech.simter.query

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.simter.query.FuzzyQuery.Companion.parseValue
import tech.simter.query.FuzzyQuery.Companion.parse
import tech.simter.query.FuzzyQuery.FuzzyValue
import tech.simter.query.FuzzyQuery.Type.And
import tech.simter.query.FuzzyQuery.Type.Or

/**
 * Test [FuzzyValue].
 *
 * @author RJ
 */
class FuzzyQueryTest {
  @Test
  fun `parseValue - single`() {
    assertEquals(FuzzyValue(Or, listOf("%abc%")), parseValue("abc"))
  }

  @Test
  fun `parseValue - or`() {
    assertEquals(FuzzyValue(Or, listOf("%a%", "%b%")), parseValue("a b"))
    assertEquals(FuzzyValue(Or, listOf("%a%", "%b%", "%c%")), parseValue("a b c"))
  }

  @Test
  fun `parseValue - and`() {
    val t = parseValue("a+b")
    assertEquals(FuzzyValue(And, listOf("%a%", "%b%")), t)
    assertEquals(FuzzyValue(And, listOf("%a%", "%b%", "%c%")), parseValue("a+b+c"))

    // and first to or
    assertEquals(FuzzyValue(And, listOf("%a b%", "%c%")), parseValue("a b+c"))
  }

  @Test
  fun `parse - single column - single value`() {
    // ilike
    assertEquals(
      FuzzyQuery(
        sql = "c ilike :search0",
        params = mapOf("search0" to "%v%"),
      ), parse(
        search = "v",
        alias = listOf("c"),
      )
    )

    // like
    assertEquals(
      FuzzyQuery(
        sql = "c like :search0",
        params = mapOf("search0" to "%v%"),
      ), parse(
        search = "v",
        alias = listOf("c"),
        ignoreCase = false,
      )
    )

    // custom paramName
    assertEquals(
      FuzzyQuery(
        sql = "c ilike :param0",
        params = mapOf("param0" to "%v%"),
      ), parse(
        search = "v",
        alias = listOf("c"),
        paramName = "param"
      )
    )

  }

  @Test
  fun `parse - single column - two values or`() {
    assertEquals(
      FuzzyQuery(
        sql = "(c ilike :search0 or c ilike :search1)",
        params = mapOf("search0" to "%v0%", "search1" to "%v1%"),
      ), parse(
        search = "v0 v1",
        alias = listOf("c"),
      )
    )
  }

  @Test
  fun `parse - single column - two values and`() {
    assertEquals(
      FuzzyQuery(
        sql = "(c ilike :search0 and c ilike :search1)",
        params = mapOf("search0" to "%v0%", "search1" to "%v1%"),
      ), parse(
        search = "v0+v1",
        alias = listOf("c"),
      )
    )
  }

  @Test
  fun `parse - two columns - single value`() {
    // ilike
    assertEquals(
      FuzzyQuery(
        sql = "(c1 ilike :search0 or c2 ilike :search0)",
        params = mapOf("search0" to "%v%"),
      ), parse(
        search = "v",
        alias = listOf("c1", "c2"),
      )
    )

    // like
    assertEquals(
      FuzzyQuery(
        sql = "(c1 like :search0 or c2 like :search0)",
        params = mapOf("search0" to "%v%"),
      ), parse(
        search = "v",
        alias = listOf("c1", "c2"),
        ignoreCase = false,
      )
    )
  }

  @Test
  fun `parse - two columns - two values or`() {
    assertEquals(
      FuzzyQuery(
        sql = "((c1 ilike :search0 or c2 ilike :search0) or (c1 ilike :search1 or c2 ilike :search1))",
        params = mapOf("search0" to "%v0%", "search1" to "%v1%"),
      ), parse(
        search = "v0 v1",
        alias = listOf("c1", "c2"),
      )
    )
  }

  @Test
  fun `parse - two columns - two values and`() {
    assertEquals(
      FuzzyQuery(
        sql = "((c1 ilike :search0 or c2 ilike :search0) and (c1 ilike :search1 or c2 ilike :search1))",
        params = mapOf("search0" to "%v0%", "search1" to "%v1%"),
      ), parse(
        search = "v0+v1",
        alias = listOf("c1", "c2"),
      )
    )
  }
}