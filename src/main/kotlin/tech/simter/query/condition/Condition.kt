package tech.simter.query.condition

/**
 * A condition interface.
 *
 * @author RJ
 */
@Deprecated(message = "Use tech.simter.query.Query.kt functions instead")
interface Condition<V> {
  val name: String
  val type: Type
  val value: V

  /**
   * The [Condition] compare type.
   *
   * @author RJ
   */
  enum class Type(val symbol: String) {
    GreaterThan(">"),
    GreaterThanOrEquals(">="),
    LessThan("<"),
    LessThanOrEquals("<="),
    Equals("="),
    NotEquals("!="),
    Like("like"),
    LikeStart("like-start"),
    LikeEnd("like-end"),
    LikeIgnoreCase("ilike"),
    LikeStartIgnoreCase("ilike-start"),
    LikeEndIgnoreCase("ilike-end"),
    In("in"),
    NotIn("not-in"),
    IsNull("is-null"),
    IsNotNull("is-not-null"),
    Between("between"),
    RangeGteLte("[]"),
    RangeGteLt("[)"),
    RangeGtLte("(]"),
    RangeGtLt("()");

    companion object {
      fun of(symbol: String): Type {
        return Type.values().find { it.symbol == symbol }
          ?: throw IllegalArgumentException("Parse '$symbol' to Type failed.")
      }
    }
  }
}