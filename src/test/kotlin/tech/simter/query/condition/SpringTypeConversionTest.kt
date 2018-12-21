package tech.simter.query.condition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig


/**
 * Test [Condition.parse].
 *
 * @author RJ
 */
@Disabled
@SpringJUnitConfig(SpringTypeConversionTest.Cfg::class)
open class SpringTypeConversionTest @Autowired constructor(
  private val conversionService: ConversionService
) {
  @Configuration
  open class Cfg {
    @Bean
    open fun conversionService(): ConversionService {
      return DefaultConversionService()
    }
  }

  @Test
  fun test() {
    assertEquals(25, conversionService.convert<Int>("25", Int::class.java) as Int)
  }
}