package com.sksamuel.elastic4s.handlers.searches.queries.text

import com.sksamuel.elastic4s.EnumConversions
import com.sksamuel.elastic4s.json.{XContentBuilder, XContentFactory}
import com.sksamuel.elastic4s.requests.searches.queries.SimpleStringQuery

object SimpleStringBodyFn {
  def apply(s: SimpleStringQuery): XContentBuilder = {
    val builder = XContentFactory.jsonBuilder().startObject("simple_query_string")
    s.queryName.foreach(builder.field("_name", _))
    s.operator.map(identity).foreach(builder.field("default_operator", _))
    s.analyzer.map(_.toString).foreach(builder.field("analyzer", _))
    s.analyzeWildcard.map(_.toString).foreach(builder.field("analyze_wildcard", _))
    s.lenient.map(_.toString).foreach(builder.field("lenient", _))
    s.minimumShouldMatch.map(_.toString).foreach(builder.field("minimum_should_match", _))
    s.autoGenerateSynonymsPhraseQuery.map(_.toString).foreach(builder.field("auto_generate_synonyms_phrase_query", _))
    s.quote_field_suffix.foreach(builder.field("quote_field_suffix", _))
    if (s.fields.nonEmpty) {
      val fields = s.fields.map {
        case (name, None)        => name
        case (name, Some(0.0D))  => name // for backwards compatibility with the erroneous code that was here
        case (name, Some(boost)) => s"$name^$boost"
      }.toArray
      builder.array("fields", fields)
    }
    if (s.flags.nonEmpty) {
      val flags = s.flags.map(EnumConversions.simpleQueryStringFlag).mkString("|")
      builder.field("flags", flags)
    }
    builder.field("query", s.query).endObject().endObject()
  }
}
