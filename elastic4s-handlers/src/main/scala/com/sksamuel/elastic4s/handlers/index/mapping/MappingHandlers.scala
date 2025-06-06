package com.sksamuel.elastic4s.handlers.index.mapping

import com.sksamuel.elastic4s.handlers.ElasticErrorParser
import com.sksamuel.elastic4s.requests.indexes.{FieldMapping, IndexFieldMapping, IndexMappings, PutMappingResponse}
import com.sksamuel.elastic4s.requests.mappings.{GetFieldMappingRequest, GetMappingRequest, PutMappingRequest}
import com.sksamuel.elastic4s.{
  ElasticError,
  ElasticRequest,
  Handler,
  HttpEntity,
  HttpResponse,
  Indexes,
  ResponseHandler
}

trait MappingHandlers {

  implicit object GetMappingHandler extends Handler[GetMappingRequest, Seq[IndexMappings]] {

    override def responseHandler: ResponseHandler[Seq[IndexMappings]] = new ResponseHandler[Seq[IndexMappings]] {

      override def handle(response: HttpResponse): Either[ElasticError, Seq[IndexMappings]] =
        response.statusCode match {
          case 201 | 200 =>
            val raw  = ResponseHandler.fromResponse[Map[String, Map[String, Map[String, Any]]]](response)
            val raw2 = raw.map {
              case (index, types) =>
                val properties = types("mappings").getOrElse("properties", Map.empty)
                val meta       = types("mappings").getOrElse("_meta", Map.empty)
                IndexMappings(index, properties.asInstanceOf[Map[String, Any]], meta.asInstanceOf[Map[String, Any]])
            }.toSeq
            Right(raw2)
          case _         =>
            try {
              Left(ElasticErrorParser.parse(response))
            } catch {
              case _: Throwable => sys.error(s"""Failed to parse error response: \n${response.toString}""")
            }
        }
    }

    override def build(request: GetMappingRequest): ElasticRequest = {
      val endpoint = request.indexes match {
        case Indexes(Nil)     => "/_mapping"
        case Indexes(indexes) => s"/${indexes.mkString(",")}/_mapping"
      }

      ElasticRequest("GET", endpoint)
    }
  }

  implicit object GetFieldMappingRequest extends Handler[GetFieldMappingRequest, Seq[IndexFieldMapping]] {
    override def responseHandler: ResponseHandler[Seq[IndexFieldMapping]] =
      new ResponseHandler[Seq[IndexFieldMapping]] {

        override def handle(response: HttpResponse): Either[ElasticError, Seq[IndexFieldMapping]] =
          response.statusCode match {
            case 201 | 200 =>
              val raw = ResponseHandler.fromResponse[Map[String, Map[String, Map[String, Map[String, Any]]]]](response)
              Right(raw.map {
                case (index, types) =>
                  val mappings = types("mappings").map {
                    case (_, mapping) =>
                      FieldMapping(mapping("full_name").toString, mapping("mapping").asInstanceOf[Map[String, Any]])
                  }.toSeq
                  IndexFieldMapping(index, mappings)
              }.toSeq)
            case _         =>
              try {
                Left(ElasticErrorParser.parse(response))
              } catch {
                case _: Throwable => sys.error(s"""Failed to parse error response: \n${response.toString}""")
              }
          }
      }

    override def build(request: GetFieldMappingRequest): ElasticRequest = {
      val baseEndpoint = request.indexes match {
        case Indexes(Nil)     => "/_mapping"
        case Indexes(indexes) => s"/${indexes.mkString(",")}/_mapping"
      }

      val endpoint = request.fields match {
        case Nil                 =>
          throw new IllegalArgumentException("Empty fields in GetFieldMappingRequest. Use GetMappingRequest instead.")
        case fields: Seq[String] => s"$baseEndpoint/field/${fields.mkString(",")}"
      }

      ElasticRequest("GET", endpoint)
    }
  }

  implicit object PutMappingHandler extends Handler[PutMappingRequest, PutMappingResponse] {

    override def build(request: PutMappingRequest): ElasticRequest = {

      val endpoint = s"/${request.indexes.values.mkString(",")}/_mapping"

      val params = scala.collection.mutable.Map.empty[String, Any]
      request.updateAllTypes.foreach(params.put("update_all_types", _))
      request.ignoreUnavailable.foreach(params.put("ignore_unavailable", _))
      request.allowNoIndices.foreach(params.put("allow_no_indices", _))
      request.expandWildcards.foreach(params.put("expand_wildcards", _))
      request.includeTypeName.foreach(params.put("include_type_name", _))

      val body   = PutMappingBuilderFn(request).string
      val entity = HttpEntity(body, "application/json")

      ElasticRequest("PUT", endpoint, params.toMap, entity)
    }
  }
}
