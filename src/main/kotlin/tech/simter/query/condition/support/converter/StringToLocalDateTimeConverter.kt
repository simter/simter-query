package tech.simter.query.condition.support.converter

import org.springframework.core.convert.converter.Converter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StringToLocalDateTimeConverter : Converter<String?, LocalDateTime?> {
  override fun convert(source: String?): LocalDateTime? {
    return if (source.isNullOrEmpty()) null
    else LocalDateTime.parse(source, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  }
}