package tech.simter.query.condition.support

import tech.simter.query.condition.Condition
import tech.simter.query.condition.Condition.Type

/**
 * A condition for 'name in (value1, value2, ...)'.
 *
 * @author RJ
 */
data class InCondition(
  override val name: String,
  override val value: List<Any>
) : Condition<List<Any>> {
  override val type: Type get() = Type.In
}