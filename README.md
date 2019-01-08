# simter-query

Some query tools. 

## 1. [Condition] encapsulation

### 1.1. The [Condition] interface 

```kotlin
interface Condition<V> {
  val name: String
  val type: Type
  val value: V

  enum class Type(val symbol: String) {
    GreaterThan         (">"),
    GreaterThanOrEquals (">="),
    LessThan            ("<"),
    LessThanOrEquals    ("<="),
    Equals              ("="),
    NotEquals           ("!="),
    Like                ("like"),
    LikeStart           ("like-start"),
    LikeEnd             ("like-end"),
    LikeIgnoreCase      ("ilike"),
    LikeStartIgnoreCase ("ilike-start"),
    LikeEndIgnoreCase   ("ilike-end"),
    In                  ("in"),
    NotIn               ("not-in"),
    IsNull              ("is-null"),
    IsNotNull           ("is-not-null"),
    Between             ("between"),
    RangeGteLte         ("[]"), // v1 <= v <= v2
    RangeGteLt          ("[)"), // v1 <= v <  v2
    RangeGtLte          ("(]"), // v1 <  v <= v2
    RangeGtLt           ("()"); // v1 <  v <  v2
  }
}
```

### 1.2. Supported Condition Implementation

| SN | Condition Implementation     | Symbol  | ValueType
|----|------------------------------|---------|-----------
|  1 | EqualsCondition              | =       | Any
|  2 | GreaterThanCondition         | >       | Any
|  3 | GreaterThanOrEqualsCondition | >=      | Any
|  4 | LessThanCondition            | <       | Any
|  5 | LessThanOrEqualsCondition    | <=      | Any
|  6 | InCondition                  | in      | List<Any>
|  7 | IsNullCondition              | is-null | -
|  8 | BetweenCondition             | between | Pair<Any, Any>

### 1.3. Condition parser

See "[ConditionUtils.parse(String) : List<Condition<*>>]".

This method parse multiple [Condition] string with structure "`[[name, stringValue, valueType, symbol], ...]`" to a [Condition] list. Each [Condition] string must has a standard `JsonArray` string format.

Default [Condition] string structure :
 1. "`[name, stringValue]`" equals to "`[name, stringValue, 'string', '=']`"
 2. "`[name, stringValue, valueType]`" equals to "`[name, stringValue, valueType, '=']`"

Supported `valueType` string :

| SN | `valueType` | Class
|----|-------------|-------
|  1 | string      | kotlin.String
|  2 | short       | kotlin.Short
|  3 | integer     | kotlin.Int
|  4 | int         | kotlin.Int
|  5 | long        | kotlin.Long
|  6 | double      | kotlin.Double
|  7 | float       | kotlin.Float
|  8 | boolean     | kotlin.Boolean
|  9 | date        | java.time.LocalDate
| 10 | time        | java.time.LocalTime
| 11 | datetime    | java.time.LocalDateTime

Supported `symbol` is "[Type.symbol]", see upper \<chapter 1.1\>.

Examples : 

| SN | Condition string                          | Condition class
|----|-------------------------------------------|-----------------
|  1 | ["k", "9", "string", "="]                 | EqualsCondition (name = "k", value = "9")
|  2 | ["k", "9", "int", "="]                    | EqualsCondition (name = "k", value = 9)
|  3 | ["k", "2018-01-10", "date", "="]          | EqualsCondition (name = "k", value = LocalDate.of(2018, 1, 10))
|  4 | ["k", "9", "int", ">"]                    | GreaterThanCondition (name = "k", value = 9)
|  5 | ["k", "[3, 6]", "int[]", "in"]<br> or ["k", "[\\"3\\", \\"6\\"]", "int[]", "in"] | InCondition (name = "k", value = listof(3, 6))
|  6 | ["k", "[\\"3\\", \\"6\\"]", "string[]", "in"] | InCondition (name = "k", value = listof("3", "6"))
|  7 | ["k", "[3, 9]", "int[]", "between"]       | BetweenCondition (name = "k", value = Pair(3, 9))
|  8 | ["k", "[3, 9]", "int[]", "[]"]            | BetweenCondition (name = "k", value = Pair(3, 9))
|  9 | ["k", "[3, 9]", "int[]", "()"]            | GreaterThanCondition (name = "k", value = 3)<br>+ LessThanCondition (name = "k", value = 9)
| 10 | ["k", "[3]", "int[]", "()"]               | GreaterThanCondition (name = "k", value = 3)
| 11 | ["k", "[\\"\\", \\"9\\"]", "int[]", "()"] | LessThanCondition (name = "k", value = 9)

```kotlin
@Test
fun example() {
  // string data
  val conditionsString = """[
    ["k", "2018-01-10", "date", "="],
    ["k", "[3]", "int[]", "()"],
    ["k", "[6, 9]", "int[]", "()"]
  ]""".trimIndent()

  // parse
  val conditions: List<Condition<*>> = ConditionUtils.parse(conditionsString)

  // verify
  assertEquals(4, conditions.size)
  assertEquals(EqualsCondition(name = "k", value = LocalDate.of(2018, 1, 10)), conditions[0])
  assertEquals(GreaterThanCondition(name = "k", value = 3), conditions[1])
  assertEquals(GreaterThanCondition(name = "k", value = 6), conditions[2])
  assertEquals(LessThanCondition(name = "k", value = 9), conditions[3])
}
```


[Condition]: https://github.com/simter/simter-query/blob/master/src/main/kotlin/tech/simter/query/condition/Condition.kt
[ConditionUtils.parse(String) : List<Condition<*>>]: https://github.com/simter/simter-query/blob/master/src/main/kotlin/tech/simter/query/condition/ConditionUtils.kt#L191
[Type.symbol]: https://github.com/simter/simter-query/blob/master/src/main/kotlin/tech/simter/query/condition/Condition.kt#L18