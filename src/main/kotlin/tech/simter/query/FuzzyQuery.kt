package tech.simter.query

/**
 * 模糊查询封装。
 */
@Deprecated(message = "Use tech.simter.query.Query.kt functions instead")
data class FuzzyQuery(
  /** 模糊查询语句 */
  val sql: String,
  /** 模糊查询的键值对 */
  val params: Map<String, Any>
) {
  /** 模糊查询类型 */
  enum class Type {
    /** 与 */
    And,

    /** 或 */
    Or
  }

  /**
   * 模糊查询值封装。
   */
  data class FuzzyValue(
    /** 模糊查询类型 */
    val type: Type,
    /** 模糊查询的值列表 */
    val values: List<Any>
  )

  companion object {
    /**
     * 解析 [search] 值为对应的模糊查询值。
     *
     * 空格分隔的值按 [或][Type.Or]处理、`+` 号分隔的值按 [与][Type.And]处理。
     */
    fun parseValue(search: String): FuzzyValue {
      // 转换全角空格和＋号为半角空格和+号
      val v = search.trim().replace("＋", "+").replace("　", " ")

      // 分割出原始的值列表
      val isOr = !v.contains('+')
      val sourceValues = if (isOr) search.split(" ") // “A空格B”的或查询
      else search.split("+") // “A+B”的与查询

      // 组合返回
      return FuzzyValue(
        type = if (isOr) Type.Or else Type.And,
        // 自动填充 % 符号到值的两端
        values = sourceValues.map { if (it.contains('%')) it else "%${it}%" }
      )
    }

    /**
     * Parse the fuzzy [search] value and generate a fuzzy sql and its bind params.
     *
     * @param[search] the fuzzy search value, can use format 'A B' or 'A+B' mode
     * @param[alias] the sql alias name to fuzzy search
     * @param[paramName] the prefix key for bind param name
     * @param[ignoreCase] whether the fuzzy search ignore case,
     */
    fun parse(
      search: String,
      alias: List<String>,
      paramName: String = "search",
      ignoreCase: Boolean = true,
    ): FuzzyQuery {
      // check arguments
      if (alias.isEmpty()) throw IllegalArgumentException("param alias could not be empty.")

      // parse values
      val fuzzyValue = parseValue(search)

      // generate sql-part and its bind value
      val symbol = if (ignoreCase) "ilike" else "like"
      val params = mutableMapOf<String, Any>()
      val sqlParts = mutableListOf<String>()
      val partPrefix = if (alias.size == 1) "" else "(" // 单个别名就无需加括号
      val partPostfix = if (alias.size == 1) "" else ")"
      fuzzyValue.values.forEachIndexed { i, value ->
        // each alias use 'or' match, such as "(t.c1 ilike :search0 or t.c2 ilike :search0)"
        sqlParts.add(
          alias.joinToString(
            separator = " or ",
            prefix = partPrefix,
            postfix = partPostfix,
          ) { "$it $symbol :$paramName$i" })

        // record bind paramName and value
        params["$paramName$i"] = value
      }

      val resultPrefix = if (fuzzyValue.values.size == 1) "" else "(" // 单个值就无需加括号
      val resultPostfix = if (fuzzyValue.values.size == 1) "" else ")"
      return FuzzyQuery(
        sql = sqlParts.joinToString(
          separator = " ${fuzzyValue.type.name.lowercase()} ",
          prefix = resultPrefix,
          postfix = resultPostfix,
        ),
        params = params,
      )
    }
  }
}
