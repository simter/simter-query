package tech.simter.query.condition.support.converter

import org.springframework.core.convert.converter.Converter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StringToLocalDateConverter : Converter<String?, LocalDate?> {
  override fun convert(source: String?): LocalDate? {
    return if (source.isNullOrEmpty()) null
    else LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE)
  }
}