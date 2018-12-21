package tech.simter.query.condition

import org.springframework.core.convert.support.DefaultConversionService
import tech.simter.query.condition.Condition.Type.*
import tech.simter.query.condition.support.*
import tech.simter.query.condition.support.converter.StringToLocalDateConverter
import tech.simter.query.condition.support.converter.StringToLocalDateTimeConverter
import tech.simter.query.condition.support.converter.StringToLocalTimeConverter
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonValue

/**
 * A condition utils.
 *
 * @author RJ
 */
object ConditionUtils {
  // use spring ConversionService to convert string value
  private val conversionService = DefaultConversionService()

  init {
    // register custom converter
    conversionService.addConverter(StringToLocalDateConverter())
    conversionService.addConverter(StringToLocalTimeConverter())
    conversionService.addConverter(StringToLocalDateTimeConverter())
  }

  // use suffix '[]' to support multiple values
  private const val MULTI_VALUE_SUFFIX = "[]"
  private val SIMPLE_VALUE_TYPES = mapOf(
    "short" to Short::class.java,
    "integer" to Int::class.java,
    "int" to Int::class.java,
    "long" to Long::class.java,
    "double" to Double::class.java,
    "float" to Float::class.java,
    "boolean" to Boolean::class.java,
    "date" to LocalDate::class.java,
    "time" to LocalTime::class.java,
    "datetime" to LocalDateTime::class.java,
    "string" to String::class.java
  )

  @Suppress("UNCHECKED_CAST")
  internal fun parseValue(stringValue: String?, valueType: String): Any? {
    if (stringValue.isNullOrEmpty()) return null
    var vt = valueType.toLowerCase()
    val isMultiValues = vt.endsWith(MULTI_VALUE_SUFFIX)
    return if (isMultiValues) { // multiple values with json array format
      vt = vt.removeSuffix(MULTI_VALUE_SUFFIX)
      val valueClass: Class<*> = SIMPLE_VALUE_TYPES[vt] ?: Class.forName(valueType)
      val values = Json.createReader(StringReader(stringValue)).readArray()
      values.mapIndexed { index, jsonValue ->
        convertValue(when (jsonValue.valueType) {
          JsonValue.ValueType.STRING -> if (values.getString(index).isNullOrEmpty()) null else values.getString(index)
          JsonValue.ValueType.NUMBER -> values.getJsonNumber(index).numberValue().toString()
          JsonValue.ValueType.TRUE -> values.getBoolean(index).toString()
          JsonValue.ValueType.FALSE -> values.getBoolean(index).toString()
          else -> throw IllegalArgumentException("Unsupported JsonValue type '${jsonValue.valueType}' in multiple values.")
        }, valueClass)
      }
    } else {                   // single value
      val valueClass: Class<*> = SIMPLE_VALUE_TYPES[vt] ?: Class.forName(valueType)
      convertValue(stringValue, valueClass)
    }
  }

  private fun convertValue(stringValue: String?, valueClass: Class<*>) =
    if (stringValue.isNullOrEmpty()) null else conversionService.convert(stringValue, valueClass)

  @Suppress("UNCHECKED_CAST")
  private fun toCondition(
    name: String,
    stringValue: String,
    valueType: String = "string",
    symbol: String = "="
  ): List<Condition<*>> {
    val value = parseValue(stringValue, valueType)
    return when (Condition.Type.of(symbol)) {
      Equals -> listOf(EqualsCondition(name = name, value = value!!))
      GreaterThanOrEquals -> listOf(GreaterThanOrEqualsCondition(name = name, value = value!!))
      GreaterThan -> listOf(GreaterThanCondition(name = name, value = value!!))
      LessThanOrEquals -> listOf(LessThanOrEqualsCondition(name = name, value = value!!))
      LessThan -> listOf(LessThanCondition(name = name, value = value!!))
      In -> listOf(InCondition(name = name, value = value as List<Any>))
      IsNull -> listOf(IsNullCondition(name = name))
      Between -> {
        value as List<Any>
        listOf(BetweenCondition(name = name, value = Pair(value[0], value[1])))
      }
      RangeGteLte -> {
        value as List<Any?>
        return when (value.size) {
          1 -> listOf(GreaterThanOrEqualsCondition(name = name, value = value[0]!!)) // >= firstValue
          2 -> {
            if (value[0] == null && value[1] != null)
              listOf(LessThanOrEqualsCondition(name = name, value = value[1]!!))     // <= secondValue
            else if (value[0] != null && value[1] == null)
              listOf(GreaterThanOrEqualsCondition(name = name, value = value[0]!!))  // >= firstValue
            else if (value[0] != null && value[1] != null)
              listOf(BetweenCondition(name = name, value = Pair(value[0]!!, value[1]!!))) // between
            else throw IllegalArgumentException("Need at lease one not null value with symbol '$symbol'.")
          }
          else -> throw IllegalArgumentException(
            "Failed to build condition with symbol '$symbol' because illegal values size of '$stringValue'."
          )
        }
      }
      RangeGteLt -> {
        value as List<Any?>
        return when (value.size) {
          1 -> listOf(GreaterThanOrEqualsCondition(name = name, value = value[0]!!)) // >= firstValue
          2 -> {
            if (value[0] == null && value[1] != null)
              listOf(LessThanCondition(name = name, value = value[1]!!))             // < secondValue
            else if (value[0] != null && value[1] == null)
              listOf(GreaterThanOrEqualsCondition(name = name, value = value[0]!!))  // >= firstValue
            else if (value[0] != null && value[1] != null)
              listOf(
                GreaterThanOrEqualsCondition(name = name, value = value[0]!!),
                LessThanCondition(name = name, value = value[1]!!)
              )
            else throw IllegalArgumentException("Need at lease one not null value with symbol '$symbol'.")
          }
          else -> throw IllegalArgumentException(
            "Failed to build condition with symbol '$symbol' because illegal values size of '$stringValue'."
          )
        }
      }
      RangeGtLte -> {
        value as List<Any?>
        return when (value.size) {
          1 -> listOf(GreaterThanCondition(name = name, value = value[0]!!))     // > firstValue
          2 -> {
            if (value[0] == null && value[1] != null)
              listOf(LessThanOrEqualsCondition(name = name, value = value[1]!!)) // <= secondValue
            else if (value[0] != null && value[1] == null)
              listOf(GreaterThanCondition(name = name, value = value[0]!!))      // > firstValue
            else if (value[0] != null && value[1] != null)
              listOf(
                GreaterThanCondition(name = name, value = value[0]!!),
                LessThanOrEqualsCondition(name = name, value = value[1]!!)
              )
            else throw IllegalArgumentException("Need at lease one not null value with symbol '$symbol'.")
          }
          else -> throw IllegalArgumentException(
            "Failed to build condition with symbol '$symbol' because illegal values size of '$stringValue'."
          )
        }
      }
      RangeGtLt -> {
        value as List<Any?>
        return when (value.size) {
          1 -> listOf(GreaterThanCondition(name = name, value = value[0]!!)) // > firstValue
          2 -> {
            if (value[0] == null && value[1] != null)
              listOf(LessThanCondition(name = name, value = value[1]!!))     // < secondValue
            else if (value[0] != null && value[1] == null)
              listOf(GreaterThanCondition(name = name, value = value[0]!!))  // > firstValue
            else if (value[0] != null && value[1] != null)
              listOf(
                GreaterThanCondition(name = name, value = value[0]!!),
                LessThanCondition(name = name, value = value[1]!!)
              )
            else throw IllegalArgumentException("Need at lease one not null value with symbol '$symbol'.")
          }
          else -> throw IllegalArgumentException(
            "Failed to build condition with symbol '$symbol' because illegal values size of '$stringValue'."
          )
        }
      }
      else -> throw IllegalArgumentException("Failed to build condition with symbol '$symbol'.")
    }
  }

  /**
   * Parse multiple [Condition] string with structure '[[name, stringValue, valueType, symbol], ...]' to a [Condition] list.
   *
   * Each [Condition] string must has a standard [JsonArray] string format.
   *
   * Default [Condition] structure:
   *
   * 1. [name, stringValue] = [name, stringValue, 'String', '=']
   * 2. [name, stringValue, valueType] = [name, stringValue, valueType, '=']
   */
  fun parse(conditionsString: String): List<Condition<*>> {
    return Json.createReader(StringReader(conditionsString)).readArray()
      .flatMap {
        it as JsonArray
        // verify data structure is [id, stringValue[, valueType, symbol]]
        if (it.size < 2) throw IllegalArgumentException("Error condition structure, condition=$it")

        // get value type
        var valueType = (if (it.size > 2) it.getString(2) else null)
        if (valueType.isNullOrEmpty()) valueType = "string"

        // get symbol
        var symbol = (if (it.size > 3) it.getString(3) else null)
        if (symbol.isNullOrEmpty()) symbol = "="

        // to condition
        toCondition(
          it.getString(0), // name
          it.getString(1), // stringValue
          valueType, // valueType
          symbol     // symbol
        )
      }
  }
}