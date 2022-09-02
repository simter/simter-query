# simter-query changelog

## 3.1.0 - 2022-09-02

- Add FuzzyQuery feature

## 3.0.0 - 2022-06-21

- Upgrade to simter-dependencies-3.0.0 (jdk-17)

## 2.0.0 - 2020-11-19

- Upgrade to simter-dependencies-2.0.0

## 1.1.0 - 2019-07-03

No code changed, just polishing maven config and unit test.

- Simplify JUnit5 config
- Change parent to simter-dependencies-1.2.0

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