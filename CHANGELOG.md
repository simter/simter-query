# simter-query changelog

## 1.0.0 - 2019-01-08

- Add Condition interface
    ```kotlin
    interface Condition<V> {
      val name: String
      val type: Type
      val value: V
    }
    ```
- Supported Condition Implementation
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