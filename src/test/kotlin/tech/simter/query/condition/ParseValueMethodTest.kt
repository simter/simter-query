package tech.simter.query.condition

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tech.simter.query.condition.ConditionUtils.parseValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Test [ConditionUtils.parseValue].
 *
 * @author RJ
 */
class ParseValueMethodTest {
  @Test
  fun `Convert to String`() {
    assertEquals("25", parseValue("25", "string"))
  }

  @Test
  fun `Convert to Boolean`() {
    assertTrue(parseValue("true", "boolean") as Boolean)
    assertTrue(parseValue("1", "boolean") as Boolean)
    assertTrue(parseValue("on", "boolean") as Boolean)
    assertTrue(parseValue("yes", "boolean") as Boolean)
    assertFalse(parseValue("false", "boolean") as Boolean)
    assertFalse(parseValue("0", "boolean") as Boolean)
    assertFalse(parseValue("off", "boolean") as Boolean)
    assertFalse(parseValue("no", "boolean") as Boolean)
  }

  @Test
  fun `Convert to Short`() {
    assertEquals(25.toShort(), parseValue("25", "short"))
  }

  @Test
  fun `Convert to Int`() {
    assertEquals(25, parseValue("25", "int"))
    assertEquals(25, parseValue("25", "integer"))
  }

  @Test
  fun `Convert to Long`() {
    assertEquals(25L, parseValue("25", "long"))
  }

  @Test
  fun `Convert to LocalDate`() {
    assertNull(parseValue("", "date"))
    assertEquals(LocalDate.of(2018, 1, 31), parseValue("2018-01-31", "date"))
  }

  @Test
  fun `Convert to LocalTime`() {
    assertNull(parseValue("", "time"))
    assertEquals(LocalTime.of(12, 1, 10), parseValue("12:01:10", "time"))
  }

  @Test
  fun `Convert to LocalDateTime`() {
    assertNull(parseValue("", "datetime"))
    assertEquals(
      LocalDateTime.of(2018, 1, 31, 12, 1, 10),
      parseValue("2018-01-31T12:01:10", "datetime")
    )
  }
}