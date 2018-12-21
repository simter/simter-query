package tech.simter.query.condition.support

import tech.simter.query.condition.Condition
import tech.simter.query.condition.Condition.Type

/**
 * A condition for 'name < value'.
 *
 * @author RJ
 */
data class LessThanCondition(
  override val name: String,
  override val value: Any
) : Condition<Any> {
  override val type: Type get() = Type.LessThan
}