package tech.simter.query.condition.support

import tech.simter.query.condition.Condition
import tech.simter.query.condition.Condition.Type

/**
 * A condition for 'name between value1 and value2'.
 *
 * @author RJ
 */
data class BetweenCondition(
  override val name: String,
  override val value: Pair<Any, Any>
) : Condition<Pair<Any, Any>> {
  override val type: Type get() = Type.Between
}