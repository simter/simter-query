package tech.simter.query.condition.support.converter

import org.springframework.core.convert.converter.Converter
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class StringToLocalTimeConverter : Converter<String?, LocalTime?> {
  override fun convert(source: String?): LocalTime? {
    return if (source.isNullOrEmpty()) null
    else LocalTime.parse(source, DateTimeFormatter.ISO_LOCAL_TIME)
  }
}