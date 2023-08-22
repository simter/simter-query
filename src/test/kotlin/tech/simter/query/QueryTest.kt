package tech.simter.query

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.simter.query.RelationType.And
import tech.simter.query.RelationType.Or
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeParseException

class QueryTest {
  @Test
  fun `toFuzzyValue - single`() {
    assertEquals(FuzzyValue(Or, listOf("%abc%")), toFuzzyValue("abc"))
  }

  @Test
  fun `toFuzzyValue - or`() {
    assertEquals(FuzzyValue(Or, listOf("%a%", "%b%")), toFuzzyValue("a b"))
    assertEquals(FuzzyValue(Or, listOf("%a%", "%b%", "%c%")), toFuzzyValue("a b c"))
  }

  @Test
  fun `toFuzzyValue - and`() {
    assertEquals(FuzzyValue(And, listOf("%a%", "%b%")), toFuzzyValue("a+b"))
    assertEquals(FuzzyValue(And, listOf("%a%", "%b%", "%c%")), toFuzzyValue("a+b+c"))

    // and first, then or
    assertEquals(FuzzyValue(And, listOf("%a b%", "%c%")), toFuzzyValue("a b+c"))
  }

  @Test
  fun `toFuzzyQueryTemplate - single column and single value`() {
    // ilike
    assertEquals(
      QueryTemplate(content = "c ilike :search_0", params = mapOf("search_0" to "%v%")),
      toFuzzyQueryTemplate(search = "v", columns = listOf("c"))
    )
    // like
    assertEquals(
      QueryTemplate(content = "c like :search_0", params = mapOf("search_0" to "%v%")),
      toFuzzyQueryTemplate(search = "v", columns = listOf("c"), ignoreCase = false)
    )
    // custom paramName
    assertEquals(
      QueryTemplate(content = "c ilike :param_0", params = mapOf("param_0" to "%v%")),
      toFuzzyQueryTemplate(search = "v", columns = listOf("c"), paramName = "param")
    )
  }

  @Test
  fun `toFuzzyQueryTemplate - single column - two values or`() {
    assertEquals(
      QueryTemplate(
        content = "(c ilike :search_0 or c ilike :search_1)",
        params = mapOf("search_0" to "%v0%", "search_1" to "%v1%")
      ),
      toFuzzyQueryTemplate(search = "v0 v1", columns = listOf("c"))
    )
  }

  @Test
  fun `toFuzzyQueryTemplate - single column - two values and`() {
    assertEquals(
      QueryTemplate(
        content = "(c ilike :search_0 and c ilike :search_1)",
        params = mapOf("search_0" to "%v0%", "search_1" to "%v1%")
      ),
      toFuzzyQueryTemplate(search = "v0+v1", columns = listOf("c"))
    )
  }

  @Test
  fun `toFuzzyQueryTemplate - two columns - single value`() {
    // ilike
    assertEquals(
      QueryTemplate(content = "(c1 ilike :search_0 or c2 ilike :search_0)", params = mapOf("search_0" to "%v%")),
      toFuzzyQueryTemplate(search = "v", columns = listOf("c1", "c2"))
    )

    // like
    assertEquals(
      QueryTemplate(content = "(c1 like :search_0 or c2 like :search_0)", params = mapOf("search_0" to "%v%")),
      toFuzzyQueryTemplate(search = "v", columns = listOf("c1", "c2"), ignoreCase = false)
    )
  }

  @Test
  fun `toFuzzyQueryTemplate - two columns - two values or`() {
    assertEquals(
      QueryTemplate(
        content = "((c1 ilike :search_0 or c2 ilike :search_0) or (c1 ilike :search_1 or c2 ilike :search_1))",
        params = mapOf("search_0" to "%v0%", "search_1" to "%v1%")
      ),
      toFuzzyQueryTemplate(search = "v0 v1", columns = listOf("c1", "c2"))
    )
  }

  @Test
  fun `toFuzzyQueryTemplate - two columns - two values and`() {
    assertEquals(
      QueryTemplate(
        content = "((c1 ilike :search_0 or c2 ilike :search_0) and (c1 ilike :search_1 or c2 ilike :search_1))",
        params = mapOf("search_0" to "%v0%", "search_1" to "%v1%")
      ),
      toFuzzyQueryTemplate(search = "v0+v1", columns = listOf("c1", "c2"))
    )
  }

  @Test
  fun `toConditionsQueryTemplate - fuzzy - error`() {
    // missing fuzzy columns
    assertThrows<IllegalArgumentException>(
      "Missing fuzzy columns config",
    ) {
      toConditionsQueryTemplate(
        conditions = toConditions("""[["fuzzy", "v"]]"""),
        columnMapper = emptyMap(),
        fuzzyColumns = emptyList(),
        ignoreCase = true
      )
    }
  }

  @Test
  fun `toConditionsQueryTemplate - fuzzy - single column single value`() {
    assertEquals(
      QueryTemplate(
        content = "c0 ilike :param_0_0",
        params = mapOf("param_0_0" to "%v%")
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["fuzzy", "v"]]"""),
        columnMapper = emptyMap(),
        fuzzyColumns = listOf("c0"),
        ignoreCase = true,
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - fuzzy - single column two values or`() {
    assertEquals(
      QueryTemplate(
        content = "(c0 ilike :param_0_0 or c0 ilike :param_0_1)",
        params = mapOf("param_0_0" to "%v0%", "param_0_1" to "%v1%")
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["fuzzy", "v0 v1"]]"""),
        columnMapper = emptyMap(),
        fuzzyColumns = listOf("c0"),
        ignoreCase = true,
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - fuzzy - single column two values and`() {
    assertEquals(
      QueryTemplate(
        content = "(c0 ilike :param_0_0 and c0 ilike :param_0_1)",
        params = mapOf("param_0_0" to "%v0%", "param_0_1" to "%v1%")
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["fuzzy", "v0+v1"]]"""),
        columnMapper = emptyMap(),
        fuzzyColumns = listOf("c0"),
        ignoreCase = true,
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - fuzzy - two columns single value`() {
    // ilike
    assertEquals(
      QueryTemplate(
        content = "(c0 ilike :param_0_0 or c1 ilike :param_0_0)",
        params = mapOf("param_0_0" to "%v%")
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["fuzzy", "v"]]"""),
        columnMapper = emptyMap(),
        fuzzyColumns = listOf("c0", "c1"),
        ignoreCase = true,
      )
    )
    // like
    assertEquals(
      QueryTemplate(
        content = "(c0 like :param_0_0 or c1 like :param_0_0)",
        params = mapOf("param_0_0" to "%v%")
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["fuzzy", "v"]]"""),
        columnMapper = emptyMap(),
        fuzzyColumns = listOf("c0", "c1"),
        ignoreCase = false,
      )
    )
    // v%
    assertEquals(
      QueryTemplate(
        content = "(c0 ilike :param_0_0 or c1 ilike :param_0_0)",
        params = mapOf("param_0_0" to "v%")
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["fuzzy", "v%"]]"""),
        columnMapper = emptyMap(),
        fuzzyColumns = listOf("c0", "c1"),
        ignoreCase = true,
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - fuzzy - two columns two values or`() {
    // ilike
    assertEquals(
      QueryTemplate(
        content = "((c0 ilike :param_0_0 or c1 ilike :param_0_0) or (c0 ilike :param_0_1 or c1 ilike :param_0_1))",
        params = mapOf("param_0_0" to "%v0%", "param_0_1" to "%v1%")
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["fuzzy", "v0 v1"]]"""),
        columnMapper = emptyMap(),
        fuzzyColumns = listOf("c0", "c1"),
        ignoreCase = true,
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - fuzzy - two columns two values and`() {
    // ilike
    assertEquals(
      QueryTemplate(
        content = "((c0 ilike :param_0_0 or c1 ilike :param_0_0) and (c0 ilike :param_0_1 or c1 ilike :param_0_1))",
        params = mapOf("param_0_0" to "%v0%", "param_0_1" to "%v1%")
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["fuzzy", "v0+v1"]]"""),
        columnMapper = emptyMap(),
        fuzzyColumns = listOf("c0", "c1"),
        ignoreCase = true,
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - =v`() {
    assertEquals(
      QueryTemplate(content = "c0 = :param_0", params = mapOf("param_0" to "v")),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "v"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - =v+fuzzy`() {
    assertEquals(
      QueryTemplate(
        content = "c0 = :param_0\nand c1 ilike :param_1_0",
        params = mapOf("param_0" to "v0", "param_1_0" to "%v1%")
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "v0"],["fuzzy", "v1"]]"""),
        columnMapper = mapOf("c" to "c0"),
        fuzzyColumns = listOf("c1"),
        ignoreCase = true,
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - gt 1`() {
    assertEquals(
      QueryTemplate(content = "c0 > :param_0", params = mapOf("param_0" to 1)),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "1", "int", ">"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - gte 1`() {
    assertEquals(
      QueryTemplate(content = "c0 >= :param_0", params = mapOf("param_0" to 1)),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "1", "int", ">="]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - lt 1`() {
    assertEquals(
      QueryTemplate(content = "c0 < :param_0", params = mapOf("param_0" to 1)),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "1", "int", "<"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - lte 1`() {
    assertEquals(
      QueryTemplate(content = "c0 <= :param_0", params = mapOf("param_0" to 1)),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "1", "int", "<="]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - in (x,y)`() {
    assertEquals(
      QueryTemplate(content = "c0 in (:param_0)", params = mapOf("param_0" to listOf(1, 2))),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", ["1", "2"], "int", "in"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - not in (x,y)`() {
    assertEquals(
      QueryTemplate(content = "c0 not in (:param_0)", params = mapOf("param_0" to listOf(1, 2))),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", ["1", "2"], "int", "not in"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  // [x, y]
  @Test
  fun `toConditionsQueryTemplate - range-gte-lte`() {
    assertEquals(
      QueryTemplate(
        content = "(c0 >= :param_0_0 and c0 <= :param_0_1)",
        params = mapOf("param_0_0" to 2, "param_0_1" to 4)
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", ["2", "4"], "int", "[]"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  // [x, y)
  @Test
  fun `toConditionsQueryTemplate - range-gte-lt`() {
    assertEquals(
      QueryTemplate(
        content = "(c0 >= :param_0_0 and c0 < :param_0_1)",
        params = mapOf("param_0_0" to 2, "param_0_1" to 4)
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", ["2", "4"], "int", "[)"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  // (x, y]
  @Test
  fun `toConditionsQueryTemplate - range-gt-lte`() {
    assertEquals(
      QueryTemplate(
        content = "(c0 > :param_0_0 and c0 <= :param_0_1)",
        params = mapOf("param_0_0" to 2, "param_0_1" to 4)
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", ["2", "4"], "int", "(]"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  // (x, y)
  @Test
  fun `toConditionsQueryTemplate - range-gt-lt`() {
    assertEquals(
      QueryTemplate(
        content = "(c0 > :param_0_0 and c0 < :param_0_1)",
        params = mapOf("param_0_0" to 2, "param_0_1" to 4)
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", ["2", "4"], "int", "()"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  // =x and [y, z]
  @Test
  fun `toConditionsQueryTemplate - =1 and range-gte2-lte3`() {
    assertEquals(
      QueryTemplate(
        content = "t.c0 = :param_0\nand (t.c1 >= :param_1_0 and t.c1 <= :param_1_1)",
        params = mapOf("param_0" to 1, "param_1_0" to 2, "param_1_1" to 3)
      ),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c0", "1", "int"],["c1", ["2", "3"], "int", "[]"]]"""),
        columnMapper = mapOf("c0" to "t.c0", "c1" to "t.c1"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - is null`() {
    assertEquals(
      QueryTemplate(content = "c0 is null", params = emptyMap()),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", null, null, "is null"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - is not null`() {
    assertEquals(
      QueryTemplate(content = "c0 is not null", params = emptyMap()),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", null, null, "is not null"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - contains`() {
    assertEquals(
      QueryTemplate(content = "c0 like :param_0", params = mapOf("param_0" to "%v%")),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "v", null, "like"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - contains ignore case`() {
    assertEquals(
      QueryTemplate(content = "c0 ilike :param_0", params = mapOf("param_0" to "%v%")),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "v", null, "ilike"]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `toConditionsQueryTemplate - gte localDate`() {
    assertEquals(
      QueryTemplate(content = "c0 >= :param_0", params = mapOf("param_0" to LocalDate.of(2020, 1, 31))),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "2020-01-31", "localDate", ">="]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
    assertEquals(
      QueryTemplate(content = "c0 >= :param_0", params = mapOf("param_0" to LocalDate.of(2020, 1, 31))),
      toConditionsQueryTemplate(
        conditions = toConditions("""[["c", "20200131|yyyyMMdd", "localDate", ">="]]"""),
        columnMapper = mapOf("c" to "c0"),
      )
    )
  }

  @Test
  fun `parseSingleValue - un supported type`() {
    assertThrows<UnsupportedOperationException>("Unsupported type 'unknown'") { parseSingleValue("a", "unknown") }
  }

  @Test
  fun `parseSingleValue - null`() {
    assertNull(parseSingleValue("", "any-type"))
  }

  @Test
  fun `parseSingleValue - string`() {
    assertEquals("any-string", parseSingleValue("any-string", "string"))
  }

  @Test
  fun `parseSingleValue - boolean`() {
    // false only on "0", "false", "f", "F", "FALSE"
    assertEquals(false, parseSingleValue("false", "boolean"))
    assertEquals(false, parseSingleValue("f", "boolean"))
    assertEquals(false, parseSingleValue("FALSE", "boolean"))
    assertEquals(false, parseSingleValue("F", "boolean"))
    assertEquals(false, parseSingleValue("0", "boolean"))
    // true
    assertEquals(true, parseSingleValue("true", "boolean"))
    assertEquals(true, parseSingleValue("t", "boolean"))
    assertEquals(true, parseSingleValue("TRUE", "boolean"))
    assertEquals(true, parseSingleValue("T", "boolean"))
    assertEquals(true, parseSingleValue("1", "boolean"))
    assertEquals(true, parseSingleValue("any-not-falsy-value", "boolean"))
  }

  @Test
  fun `parseSingleValue - short`() {
    assertThrows<NumberFormatException> { parseSingleValue("not-number", "short") }
    assertEquals(0.toShort(), parseSingleValue("0", "short"))
    assertEquals(1.toShort(), parseSingleValue("1", "short"))
    assertEquals((-1).toShort(), parseSingleValue("-1", "short"))
    assertEquals(Short.MAX_VALUE, parseSingleValue(Short.MAX_VALUE.toString(), "short"))
    assertEquals(Short.MIN_VALUE, parseSingleValue(Short.MIN_VALUE.toString(), "short"))
  }

  @Test
  fun `parseSingleValue - int`() {
    // int
    assertThrows<NumberFormatException> { parseSingleValue("not-number", "int") }
    assertEquals(0, parseSingleValue("0", "int"))
    assertEquals(1, parseSingleValue("1", "int"))
    assertEquals(-1, parseSingleValue("-1", "int"))
    assertEquals(Integer.MAX_VALUE, parseSingleValue(Integer.MAX_VALUE.toString(), "int"))
    assertEquals(Integer.MIN_VALUE, parseSingleValue(Integer.MIN_VALUE.toString(), "int"))
    // integer
    assertThrows<NumberFormatException> {
      parseSingleValue(
        "not-number",
        "integer"
      )
    }
    assertEquals(0, parseSingleValue("0", "integer"))
    assertEquals(1, parseSingleValue("1", "integer"))
    assertEquals(-1, parseSingleValue("-1", "integer"))
    assertEquals(Integer.MAX_VALUE, parseSingleValue(Integer.MAX_VALUE.toString(), "integer"))
    assertEquals(Integer.MIN_VALUE, parseSingleValue(Integer.MIN_VALUE.toString(), "integer"))
  }

  @Test
  fun `parseSingleValue - long`() {
    assertThrows<NumberFormatException> { parseSingleValue("not-number", "long") }
    assertEquals(0L, parseSingleValue("0", "long"))
    assertEquals(1L, parseSingleValue("1", "long"))
    assertEquals(-1L, parseSingleValue("-1", "long"))
    assertEquals(Long.MAX_VALUE, parseSingleValue(Long.MAX_VALUE.toString(), "long"))
    assertEquals(Long.MIN_VALUE, parseSingleValue(Long.MIN_VALUE.toString(), "long"))
  }

  @Test
  fun `parseSingleValue - float`() {
    assertThrows<NumberFormatException> { parseSingleValue("not-number", "float") }
    assertEquals(0f, parseSingleValue("0", "float"))
    assertEquals(0.001f, parseSingleValue("0.001", "float"))
    assertEquals(1.234f, parseSingleValue("1.234", "float"))
    assertEquals(-1.234f, parseSingleValue("-1.234", "float"))
    assertEquals(Float.MAX_VALUE, parseSingleValue(Float.MAX_VALUE.toString(), "float"))
    assertEquals(Float.MIN_VALUE, parseSingleValue(Float.MIN_VALUE.toString(), "float"))
  }

  @Test
  fun `parseSingleValue - double`() {
    assertThrows<NumberFormatException> {
      parseSingleValue(
        "not-number",
        "double"
      )
    }
    assertEquals(0.0, parseSingleValue("0", "double"))
    assertEquals(0.001, parseSingleValue("0.001", "double"))
    assertEquals(1.234, parseSingleValue("1.234", "double"))
    assertEquals(-1.234, parseSingleValue("-1.234", "double"))
    assertEquals(Double.MAX_VALUE, parseSingleValue(Double.MAX_VALUE.toString(), "double"))
    assertEquals(Double.MIN_VALUE, parseSingleValue(Double.MIN_VALUE.toString(), "double"))
  }

  @Test
  fun `parseSingleValue - number`() {
    // number equals to double
    assertThrows<NumberFormatException> {
      parseSingleValue(
        "not-number",
        "number"
      )
    }
    assertEquals(0.0, parseSingleValue("0", "number"))
    assertEquals(0.001, parseSingleValue("0.001", "number"))
    assertEquals(1.234, parseSingleValue("1.234", "number"))
    assertEquals(-1.234, parseSingleValue("-1.234", "number"))
    assertEquals(Double.MAX_VALUE, parseSingleValue(Double.MAX_VALUE.toString(), "number"))
    assertEquals(Double.MIN_VALUE, parseSingleValue(Double.MIN_VALUE.toString(), "number"))
  }

  @Test
  fun `parseSingleValue - bigDecimal`() {
    assertThrows<NumberFormatException> {
      parseSingleValue(
        "not-number",
        "bigDecimal"
      )
    }
    assertEquals(BigDecimal("0"), parseSingleValue("0", "bigDecimal"))
    assertEquals(BigDecimal("0.00"), parseSingleValue("0.00", "bigDecimal"))
    assertEquals(BigDecimal("0.001"), parseSingleValue("0.001", "bigDecimal"))
    assertEquals(BigDecimal("1.234"), parseSingleValue("1.234", "bigDecimal"))
    assertEquals(BigDecimal("-1.234"), parseSingleValue("-1.234", "bigDecimal"))
  }

  @Test
  fun `parseSingleValue - localDate`() {
    // localDate
    assertThrows<DateTimeParseException> { parseSingleValue("not-date", "localDate") }
    assertEquals(LocalDate.of(2020, 1, 1), parseSingleValue("2020-01-01", "localDate"))
    assertEquals(LocalDate.of(2020, 1, 31), parseSingleValue("2020-01-31", "localDate"))
    assertEquals(LocalDate.of(2020, 12, 31), parseSingleValue("2020-12-31", "localDate"))
    // custom patten
    assertEquals(LocalDate.of(2020, 1, 31), parseSingleValue("20200131|yyyyMMdd", "localDate"))
    // date
    assertThrows<DateTimeParseException> { parseSingleValue("not-date", "date") }
    assertEquals(LocalDate.of(2020, 1, 1), parseSingleValue("2020-01-01", "date"))
    assertEquals(LocalDate.of(2020, 1, 31), parseSingleValue("2020-01-31", "date"))
    assertEquals(LocalDate.of(2020, 12, 31), parseSingleValue("2020-12-31", "date"))
  }

  @Test
  fun `parseSingleValue - localTime`() {
    // localTime
    assertThrows<DateTimeParseException> { parseSingleValue("not-time", "localTime") }
    assertThrows<DateTimeParseException> { parseSingleValue("00", "localTime") }
    assertEquals(LocalTime.of(0, 0), parseSingleValue("00:00", "localTime"))
    assertEquals(LocalTime.of(0, 0, 0), parseSingleValue("00:00:00", "localTime"))
    assertEquals(LocalTime.of(6, 59, 59), parseSingleValue("06:59:59", "localTime"))
    assertEquals(LocalTime.of(23, 59, 59), parseSingleValue("23:59:59", "localTime"))
    // custom patten
    assertEquals(LocalTime.of(23, 1, 59), parseSingleValue("230159|HHmmss", "localTime"))
    // time
    assertThrows<DateTimeParseException> { parseSingleValue("not-time", "time") }
    assertThrows<DateTimeParseException> { parseSingleValue("00", "time") }
    assertEquals(LocalTime.of(0, 0), parseSingleValue("00:00", "time"))
    assertEquals(LocalTime.of(0, 0, 0), parseSingleValue("00:00:00", "time"))
    assertEquals(LocalTime.of(6, 59, 59), parseSingleValue("06:59:59", "time"))
    assertEquals(LocalTime.of(23, 59, 59), parseSingleValue("23:59:59", "time"))
  }

  @Test
  fun `parseSingleValue - localDateTime`() {
    // localDateTime
    assertThrows<DateTimeParseException> { parseSingleValue("not-date-time", "localDateTime") }
    assertThrows<DateTimeParseException> { parseSingleValue("00", "localDateTime") }
    assertEquals(
      LocalDateTime.of(2020, 1, 31, 0, 0, 0),
      parseSingleValue("2020-01-31T00:00:00", "localDateTime")
    )
    assertEquals(
      LocalDateTime.of(2020, 1, 31, 0, 0, 0),
      parseSingleValue("2020-01-31T00:00", "localDateTime")
    )
    // custom patten
    assertEquals(
      LocalDateTime.of(2020, 1, 31, 0, 0, 0),
      parseSingleValue("2020/1/31 00:00|yyyy/M/d HH:mm", "localDateTime")
    )
    // dateTime
    assertThrows<DateTimeParseException> { parseSingleValue("not-date-time", "dateTime") }
    assertThrows<DateTimeParseException> { parseSingleValue("00", "dateTime") }
    assertEquals(
      LocalDateTime.of(2020, 1, 31, 0, 0, 0),
      parseSingleValue("2020-01-31T00:00:00", "dateTime")
    )
    assertEquals(
      LocalDateTime.of(2020, 1, 31, 0, 0, 0),
      parseSingleValue("2020-01-31T00:00", "dateTime")
    )
  }

  @Test
  fun `parseSingleValue - yearMonth`() {
    assertThrows<DateTimeParseException> { parseSingleValue("not-ym", "yearMonth") }
    assertEquals(YearMonth.of(2020, 1), parseSingleValue("2020-01", "yearMonth"))
    assertEquals(YearMonth.of(2020, 12), parseSingleValue("2020-12", "yearMonth"))
    // custom patten
    assertEquals(YearMonth.of(2020, 1), parseSingleValue("202001|yyyyMM", "yearMonth"))
  }
}