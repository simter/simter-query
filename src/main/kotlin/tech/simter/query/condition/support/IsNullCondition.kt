package tech.simter.query.condition.support

import tech.simter.query.condition.Condition
import tech.simter.query.condition.Condition.Type

/**
 * A condition for 'name is null'.
 *
 * @author RJ
 */
data class IsNullCondition(
  override val name: String
) : Condition<Any> {
  override val type: Type get() = Type.IsNull
  override val value: Any get() = UnsupportedOperationException()
}