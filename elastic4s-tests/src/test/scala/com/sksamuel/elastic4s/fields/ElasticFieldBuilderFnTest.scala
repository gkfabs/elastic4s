package com.sksamuel.elastic4s.fields

import com.sksamuel.elastic4s.handlers.fields.ElasticFieldBuilderFn
import com.sksamuel.elastic4s.jackson.JacksonSupport
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ElasticFieldBuilderFnTest extends AnyWordSpec with Matchers {

  "ElasticFieldBuilderFn" should {

    "support CompletionField without contexts" in {
      val field = CompletionField(
        "theField",
        copyTo = Seq("a", "bc"),
        maxInputLength = Some(13),
        preservePositionIncrements = Some(true)
      )

      val jsonString =
        """{"type":"completion","copy_to":["a","bc"],"preserve_position_increments":true,"max_input_length":13}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "support CompletionField with contexts" in {
      val field = CompletionField(
        "theField",
        copyTo = Seq("a", "bc"),
        maxInputLength = Some(13),
        preservePositionIncrements = Some(true),
        contexts = Seq(ContextField("location", "geo", precision = Some(4), path = Some("loc")))
      )

      val jsonString =
        """{"type":"completion","copy_to":["a","bc"],"preserve_position_increments":true,"max_input_length":13,"contexts":[{"name":"location","type":"geo","path":"loc","precision":4}]}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "support KeywordField without subfields" in {
      val field      = KeywordField(
        "keyField",
        splitQueriesOnWhitespace = Some(true),
        ignoreAbove = Some(13),
        norms = Some(true),
        normalizer = Some("uppercase")
      )
      val jsonString =
        """{"type":"keyword","ignore_above":13,"norms":true,"normalizer":"uppercase","split_queries_on_whitespace":true}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }
    "support KeywordField with subfields" in {
      val field      = KeywordField(
        "keyField",
        fields = List(
          TextField("fullText", eagerGlobalOrdinals = Some(true)),
          KeywordField("lower_keys", normalizer = Some("lowercase"))
        )
      )
      val jsonString =
        """{"type":"keyword","fields":{"fullText":{"type":"text","eager_global_ordinals":true},"lower_keys":{"type":"keyword","normalizer":"lowercase"}}}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "support ObjectField with subfields" in {
      val field      = ObjectField(
        "obj",
        properties = Seq(
          TextField("fullName", eagerGlobalOrdinals = Some(true)),
          KeywordField("lastName", normalizer = Some("lowercase"))
        )
      )
      val jsonString =
        """{"type":"object","properties":{"fullName":{"type":"text","eager_global_ordinals":true},"lastName":{"type":"keyword","normalizer":"lowercase"}}}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "support FloatRangeField" in {
      val field      = FloatRangeField("floatRange", index = Some(true))
      val jsonString = """{"type":"float_range","index":true}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "support DateRangeField" in {
      val field      = DateRangeField("time_range", format = Some("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"))
      val jsonString = """{"type":"date_range","format":"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }
    "Support ScaledFloat" in {
      val field      = ScaledFloatField("scaled_floating_field", scalingFactor = Some(100), nullValue = Some(13.6F))
      val jsonString = """{"type":"scaled_float","null_value":13.600000381469727,"scaling_factor":100}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "Support UnsignedLong" in {
      val field      = UnsignedLongField("unsigned_long_field", nullValue = Some(1647995L))
      val jsonString = """{"type":"unsigned_long","null_value":1647995}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "construct a field without type but with properties" in {
      val jsonString =
        """{"properties":{"fullName":{"type":"text","eager_global_ordinals":true},"lastName":{"type":"keyword","normalizer":"lowercase"}}}"""
      ElasticFieldBuilderFn.construct(
        "test",
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe ObjectField(
        "test",
        properties = Seq(
          TextField("fullName", eagerGlobalOrdinals = Some(true)),
          KeywordField("lastName", normalizer = Some("lowercase"))
        )
      )
    }

    "throw a RuntimeException if a field cannot be constructed" in {
      val jsonString =
        """{"properties":{"fullName":{"type":"text","eager_global_ordinals":true},"lastName":{"type":"dynamic","normalizer":"lowercase"}}}"""
      val ex         = intercept[RuntimeException] {
        ElasticFieldBuilderFn.construct("test", JacksonSupport.mapper.readValue[Map[String, Any]](jsonString))
      }
      ex.getMessage shouldBe "Could not convert mapping for 'lastName' to an ElasticField"

    }

    "support AnnotatedTextField" in {
      val field      = AnnotatedTextField("annotatedText", copyTo = Seq("a", "bc"))
      val jsonString = """{"type":"annotated_text","copy_to":["a","bc"]}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field

    }

    "support AnnotatedTextField analyzers" in {
      val field      = AnnotatedTextField(
        "annotatedText",
        analyzer = Some("standard"),
        searchAnalyzer = Some("standard"),
        searchQuoteAnalyzer = Some("standard")
      )
      val jsonString =
        """{"type":"annotated_text","analyzer":"standard","search_analyzer":"standard","search_quote_analyzer":"standard"}"""

      ElasticFieldBuilderFn(field).string shouldBe (jsonString)
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe (field)

      val fieldSet =
        AnnotatedTextField("annotatedText").analyzer("standard").searchAnalyzer("standard").searchQuoteAnalyzer(
          "standard"
        )
      ElasticFieldBuilderFn(fieldSet).string shouldBe (jsonString)
      ElasticFieldBuilderFn.construct(
        fieldSet.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe (fieldSet)
    }

    "support DenseVectorField" in {
      val field      = DenseVectorField(
        "dense_vector_field",
        elementType = Some("byte"),
        dims = Some(3),
        index = Some(true),
        indexOptions = Some(DenseVectorIndexOptions(DenseVectorField.Flat))
      )
      val jsonString =
        """{"type":"dense_vector","element_type":"byte","dims":3,"index":true,"index_options":{"type":"flat"}}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "support DenseVectorField with similarity" in {
      val field      = DenseVectorField(
        "dense_vector_field",
        elementType = Some("byte"),
        dims = Some(3),
        index = Some(true),
        similarity = Some(MaxInnerProduct)
      )
      val jsonString =
        """{"type":"dense_vector","element_type":"byte","dims":3,"index":true,"similarity":"max_inner_product"}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "support DenseVectorField with all index options" in {
      val field      = DenseVectorField(
        "dense_vector_field",
        elementType = Some("byte"),
        dims = Some(3),
        index = Some(true),
        indexOptions = Some(DenseVectorIndexOptions(DenseVectorField.Int8Hnsw, Some(100), Some(200), Some(0.5F)))
      )
      val jsonString =
        """{"type":"dense_vector","element_type":"byte","dims":3,"index":true,"index_options":{"type":"int8_hnsw","m":100,"ef_construction":200,"confidence_interval":0.5}}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }

    "support SparseVectorField" in {
      val field      = SparseVectorField("sparse_vector_field")
      val jsonString = """{"type":"sparse_vector"}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }
    "support RankFeaturesField with positive_score_impact" in {
      val field      = RankFeaturesField("rank_features_field", positiveScoreImpact = Some(false))
      val jsonString = """{"type":"rank_features","positive_score_impact":false}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }
    "support RankVectorsField" in {
      val field      = RankVectorsField("rank_vectors_field", elementType = Some("byte"), dims = Some(3))
      val jsonString = """{"type":"rank_vectors","element_type":"byte","dims":3}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }
    "support SemanticTextField" in {
      val field      = SemanticTextField("semantic_text_field", "my-elser-endpoint")
      val jsonString = """{"type":"semantic_text","inference_id":"my-elser-endpoint"}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }
    "support SemanticTextField with search_inference_id" in {
      val field      = SemanticTextField("semantic_text_field", "my-elser-endpoint", Some("my-elser-endpoint-for-search"))
      val jsonString =
        """{"type":"semantic_text","inference_id":"my-elser-endpoint","search_inference_id":"my-elser-endpoint-for-search"}"""
      ElasticFieldBuilderFn(field).string shouldBe jsonString
      ElasticFieldBuilderFn.construct(
        field.name,
        JacksonSupport.mapper.readValue[Map[String, Any]](jsonString)
      ) shouldBe field
    }
  }
}
