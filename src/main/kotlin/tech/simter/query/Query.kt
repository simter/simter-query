package tech.simter.query

import java.io.StringReader
import java.lang.Double.parseDouble
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt
import java.lang.Long.parseLong
import java.lang.Short.parseShort
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter.*
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonValue.ValueType.NULL

/** A relation type */
enum class RelationType(val symbol: String) {
  And("and"),
  Or("or"),
}

/** Operator enum */
enum class Operator(val symbol: String) {
  EQ("="),
  NOT_EQ("!="),
  GT(">"),
  GTE(">="),
  LT("<"),
  LTE("<="),
  IN("in"),
  NOT_IN("not in"),
  RANGE_EQ_EQ("[]"),
  RANGE_EQ_LT("[)"),
  RANGE_GT_EQ("(]"),
  RANGE_GT_LT("()"),
  IS_NULL("is null"),
  IS_NOT_NULL("is not null"),
  CONTAINS("like"),
  CONTAINS_IGNORE_CASE("ilike");

  companion object {
    fun of(symbol: String): Operator {
      return Operator.values().find { it.symbol == symbol }
        ?: throw IllegalArgumentException("Parse '$symbol' to Operator failed.")
    }
  }
}

/** A fuzzy value.
 * 1. "A+B" as And relation
 * 2. "A B" as Or relation
 * 3. Each values include '%' symbol
 */
data class FuzzyValue(
  /** Fuzzy type */
  val type: RelationType,
  /** Fuzzy values */
  val values: List<Any>
)

/** A query template */
data class QueryTemplate(
  /** sql template string. Such as "a.c = :value and ..." */
  val content: String,
  /** binding key-value pairs */
  val params: Map<String, Any?>
)

/** A condition */
data class Condition(
  /** The condition id */
  val id: String,
  /** The condition value. single or multiple values */
  val value: Any?,
  /** The Conditional comparator */
  val operator: Operator,
)

/**
 * Parse the search value to a FuzzyValue structure.
 * 1. 'A+B' deal as And relation
 * 2. 'A B' deal as Or relation
 * @param search the raw fuzzy value
 */
fun toFuzzyValue(search: String): FuzzyValue {
  // split raw value by '+' or ' '
  val isOr = !search.contains("+")
  val sourceValues = if (isOr) search.split(" ") // 'A B' deal as Or relation
  else search.split("+") // 'A+B' deal as And relation

  // combine
  return FuzzyValue(
    type = if (isOr) RelationType.Or else RelationType.And,
    // auto fill '%' symbol
    values = sourceValues.map { if (it.contains("%")) it else "%$it%" }
  )
}

/**
 * Parse the fuzzy value and generate a fuzzy sql template and its binding params.
 *
 * The binding param markup use `${paramName}` format for <https://deno.land/x/postgresjs>.
 *
 * @param[search] the raw fuzzy value, single, 'A+B' or 'A B' mode
 * @param[columns] the sql column name to fuzzy search
 * @param[paramName] the query param name, default "search"
 * @param[ignoreCase] whether ignore case, default true
 */
fun toFuzzyQueryTemplate(
  search: String,
  columns: List<String>,
  paramName: String = "search",
  ignoreCase: Boolean = true,
): QueryTemplate {
  // check arguments
  if (columns.isEmpty()) throw IllegalArgumentException("param alias could not be empty.")

  // parse values
  val fuzzyValue = toFuzzyValue(search)

  // generate sql-part and its bind value
  val symbol = if (ignoreCase) "ilike" else "like"
  val params = mutableMapOf<String, Any>()
  val sqlParts = mutableListOf<String>()
  val partPrefix = if (columns.size == 1) "" else "(" // single column not need ()
  val partPostfix = if (columns.size == 1) "" else ")"
  fuzzyValue.values.forEachIndexed { i, value ->
    // each alias use 'or' match, such as "(t.c1 ilike :search0 or t.c2 ilike :search0)"
    sqlParts.add(partPrefix + columns.joinToString(" or ") { "$it $symbol :${paramName}_${i}" } + partPostfix)

    // record bind paramName and value
    params["${paramName}_${i}"] = value
  }

  val resultPrefix = if (fuzzyValue.values.size == 1) "" else "(" // single value not need ()
  val resultPostfix = if (fuzzyValue.values.size == 1) "" else ")"
  return QueryTemplate(
    content = resultPrefix + sqlParts.joinToString(" ${fuzzyValue.type.symbol} ") + resultPostfix,
    params = params,
  )
}

/** Parse an array string value to the specify type array value */
private fun parseMultipleValue(values: List<String>, type: String): List<Any?> {
  return values.map { value -> parseSingleValue(value, type) }
}

private val FALSY_VALUES = listOf("0", "false", "f", "F", "FALSE")

/** Parse a string value to the specify type value.
 *
 * Note: Empty string always return null.
 */
fun parseSingleValue(value: String, type: String): Any? {
  if (value == "") return null
  return when (type) {
    "string" -> value
    "boolean" -> !FALSY_VALUES.contains(value)
    "short" -> parseShort(value)
    "int", "integer" -> parseInt(value)
    "long" -> parseLong(value)
    "float" -> parseFloat(value)
    "double", "number" -> parseDouble(value)
    "bigDecimal", "decimal", "numeric", "money" -> BigDecimal(value)
    "localDate", "date" -> parseLocalDate(value)
    "localTime", "time" -> parseLocalTime(value)
    "localDateTime", "dateTime" -> parseLocalDateTime(value)
    "yearMonth" -> parseYearMonth(value)
    else -> throw UnsupportedOperationException("Unsupported type '${type}'")
  }
}

/**
 * Parse a string value to a LocalDate.
 *
 * 1. Empty or null value always return null.
 * 2. Default parse as [ISO_LOCAL_DATE] format.
 * 3. If [value] have format "$value|$patten", such as "2020/1/20|yyyy/M/d", parse as $patten format
 */
private fun parseLocalDate(value: String?): LocalDate? {
  if (value.isNullOrEmpty()) return null
  if (!value.contains("|")) return LocalDate.parse(value, ISO_LOCAL_DATE)

  // deal "$value|$patten" format
  val vp = value.split("|")
  return LocalDate.parse(vp[0], ofPattern(vp[1]))
}

/**
 * Parse a string value to a LocalTime.
 *
 * 1. Empty or null value always return null.
 * 2. Default parse as [ISO_LOCAL_TIME] format.
 * 3. If [value] have format "$value|$patten", such as "12:05:32|HH:mm:ss", parse as $patten format
 */
private fun parseLocalTime(value: String?): LocalTime? {
  if (value.isNullOrEmpty()) return null
  if (!value.contains("|")) return LocalTime.parse(value, ISO_LOCAL_TIME)

  // deal "$value|$patten" format
  val vp = value.split("|")
  return LocalTime.parse(vp[0], ofPattern(vp[1]))
}

/**
 * Parse a string value to a LocalDateTime.
 *
 * 1. Empty or null value always return null.
 * 2. Default parse as [ISO_LOCAL_DATE_TIME] format.
 * 3. If [value] have format "$value|$patten", such as "2020/1/10 12:05|yyyy/M/d HH:mm", parse as $patten format
 */
private fun parseLocalDateTime(value: String?): LocalDateTime? {
  if (value.isNullOrEmpty()) return null
  if (!value.contains("|")) return LocalDateTime.parse(value, ISO_LOCAL_DATE_TIME)

  // deal "$value|$patten" format
  val vp = value.split("|")
  return LocalDateTime.parse(vp[0], ofPattern(vp[1]))
}

/**
 * Parse a string value to a [YearMonth].
 *
 * 1. Empty or null value always return null.
 * 2. Default parse as "yyyy-MM" format.
 * 3. If [value] have format "$value|$patten", such as "202001|yyyyMM", parse as $patten format
 */
fun parseYearMonth(value: String?): YearMonth? {
  if (value.isNullOrEmpty()) return null
  if (!value.contains("|")) return YearMonth.parse(value, ofPattern("yyyy-MM"))

  // deal "$value|$patten" format
  val vp = value.split("|")
  return YearMonth.parse(vp[0], ofPattern(vp[1]))
}

/**
 * Convert a condition to a QueryTemplate.
 *
 * - If the [condition] is a fuzzy search condition, [fuzzyColumns] is required, others throw [IllegalArgumentException].
 * - The [ignoreCase] param only use for fuzzy search condition.
 */
private fun toConditionQueryTemplate(
  condition: Condition,
  paramName: String = "param",
  fuzzyColumns: List<String> = emptyList(),
  ignoreCase: Boolean = true,
): QueryTemplate {
  val (id, value, operator) = condition

  // fuzzy condition
  if (id == "fuzzy") {
    if (fuzzyColumns.isEmpty()) throw IllegalArgumentException("Missing fuzzy columns config")
    return toFuzzyQueryTemplate(value as String, fuzzyColumns, paramName, ignoreCase)
  }

  // not fuzzy condition
  val params = mutableMapOf<String, Any?>()
  // multiple values compare
  if (arrayOf(Operator.IN, Operator.NOT_IN).contains(operator)) {
    // in, not in
    params[paramName] = value
    return QueryTemplate(
      // such as "age in :param"
      content = "$id ${operator.symbol} (:$paramName)",
      params,
    )
  } else if (arrayOf(Operator.IS_NULL, Operator.IS_NOT_NULL).contains(operator)) {
    // is null, is not null
    return QueryTemplate(
      // such as "name is null"
      content = "$id ${operator.symbol}",
      params,
    )
  } else if (arrayOf(Operator.RANGE_EQ_EQ, Operator.RANGE_EQ_LT, Operator.RANGE_GT_EQ, Operator.RANGE_GT_LT)
      .contains(operator)
  ) {
    // range compare
    val vs = value as List<Any?>
    val has0 = vs.isNotEmpty() && vs[0] != null
    val has1 = vs.size > 1 && vs[1] != null
    val both = has0 && has1
    val contents = mutableListOf<String>()
    if (has0) {
      params[paramName + "_0"] = vs[0]
      contents.add("$id ${if (operator.symbol.startsWith("[")) ">=" else ">"} :${paramName + "_0"}")
    }
    if (has1) {
      params[paramName + "_1"] = vs[1]
      contents.add("$id ${if (operator.symbol.endsWith("]")) "<=" else "<"} :${paramName + "_1"}")
    }
    return QueryTemplate(
      content = (if (both) "(" else "") + contents.joinToString(" and ") + (if (both) ")" else ""),
      params,
    )
  } else {
    // single value compare
    if (arrayOf(Operator.CONTAINS, Operator.CONTAINS_IGNORE_CASE).contains(operator)) {
      // single value like or ilike
      value as String
      params[paramName] = if (value.contains("%")) value else "%$value%"
    } else params[paramName] = value
    return QueryTemplate(
      content = "$id ${operator.symbol} :${paramName}",
      params = params,
    )
  }
}

/**
 * Convert multiple conditions to QueryTemplate.
 *
 * All conditions column should have mapping to avoid sql inject attack, otherwise throw Error.
 */
fun toConditionsQueryTemplate(
  conditions: List<Condition>,
  columnMapper: Map<String, String>,
  fuzzyColumns: List<String> = emptyList(),
  paramName: String = "param",
  ignoreCase: Boolean = true,
): QueryTemplate {
  if (conditions.isEmpty()) throw IllegalArgumentException("Empty conditions")

  // check to avoid sql inject attack: each column must have mapping
  for ((column, _, _) in conditions) {
    if (column != "fuzzy" && !columnMapper.containsKey(column)) {
      throw IllegalArgumentException("Missing column '${column}' mapping")
    }
  }

  // build condition template
  val templates = mutableListOf<QueryTemplate>()
  conditions.forEachIndexed { i, c ->
    templates.add(
      toConditionQueryTemplate(
        condition = if (c.id == "fuzzy") c else c.copy(id = columnMapper[c.id] as String),
        paramName = "${paramName}_${i}",
        fuzzyColumns,
        ignoreCase
      )
    )
  }

  // combine all templates
  return templates.reduce { pre, cur ->
    QueryTemplate(
      content = "${pre.content + "\nand "}${cur.content}",
      params = pre.params + cur.params,
    )
  }
}

/**
 * Parse multiple [Condition] string with structure '[[name, stringValue, valueType, symbol], ...]' to a [Condition] list.
 *
 * Each [Condition] string must have a standard [JsonArray] string format.
 *
 * Default [Condition] structure:
 *
 * 1. [name, stringValue] = [name, stringValue, 'String', '=']
 * 2. [name, stringValue, valueType] = [name, stringValue, valueType, '=']
 */
fun toConditions(conditionsString: String): List<Condition> {
  return Json.createReader(StringReader(conditionsString)).readArray()
    .map {
      it as JsonArray
      // verify data structure is [id, stringValue[, valueType, symbol]]
      if (it.size < 2) throw IllegalArgumentException("Error condition structure, condition=$it")

      // get value type
      var valueType = (if (it.size > 2) (if (it[2].valueType != NULL) it.getString(2) else null) else null)
      if (valueType.isNullOrEmpty()) valueType = "string"

      // get symbol
      var symbol = (if (it.size > 3) it.getString(3) else null)
      if (symbol.isNullOrEmpty()) symbol = "="

      // to condition
      val value = it[1]
      if (value.valueType == NULL) { // null value
        Condition(
          id = it.getString(0),
          operator = Operator.of(symbol),
          value = null,
        )
      } else if (value is JsonArray) {  // multiple values
        Condition(
          id = it.getString(0),
          operator = Operator.of(symbol),
          value = parseMultipleValue(value.mapIndexed { i, _ -> value.getString(i) }, valueType),
        )
      } else { // single value
        Condition(
          id = it.getString(0),
          operator = Operator.of(symbol),
          value = parseSingleValue(it.getString(1), valueType),
        )
      }
    }
}