package com.sksamuel.elastic4s.requests.searches.queries

import com.sksamuel.elastic4s.JsonSugar
import com.sksamuel.elastic4s.handlers.searches.queries
import com.sksamuel.elastic4s.handlers.searches.queries.IntervalsQueryBuilderFn
import com.sksamuel.elastic4s.requests.script.Script
import org.scalatest.GivenWhenThen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class IntervalsQueryBuilderFnTest extends AnyFunSuite with Matchers with GivenWhenThen with JsonSugar {
  test("Should correctly build intervals query") {
    Given("An intervals query")
    val query = IntervalsQuery(
      "my_text",
      AllOf(List(
        Match(query = "my favorite food").maxGaps(0).ordered(true),
        AnyOf(intervals =
          List(
            Match(query = "hot water"),
            Match(query = "cold porridge")
          )
        )
      )).ordered(true)
    )

    When("Intervals query is built")
    val queryBody = IntervalsQueryBuilderFn(query)

    Then("query should have right fields")
    queryBody.string should matchJson(intervalsQuery)
  }

  def intervalsQuery: String =
    """
      |{
      |  "intervals" : {
      |    "my_text" : {
      |      "all_of" : {
      |        "ordered" : true,
      |        "intervals" : [
      |          {
      |            "match" : {
      |              "query" : "my favorite food",
      |              "max_gaps" : 0,
      |              "ordered" : true
      |            }
      |          },
      |          {
      |            "any_of" : {
      |              "intervals" : [
      |                { "match" : { "query" : "hot water" } },
      |                { "match" : { "query" : "cold porridge" } }
      |              ]
      |            }
      |          }
      |        ]
      |      }
      |    }
      |  }
      |}
    """.stripMargin.replace("\n", "")

  test("Should correctly build intervals query with a filter") {
    Given("An intervals query with a filter")
    val query = IntervalsQuery(
      "my_text",
      Match(query = "hot porridge").maxGaps(10).filter(
        IntervalsFilter().notContaining(Match(query = "salty"))
      )
    )

    When("Intervals query is built")
    val queryBody = queries.IntervalsQueryBuilderFn(query)

    Then("query should have right fields")
    queryBody.string should matchJson(intervalsWithFilterQuery)
  }

  def intervalsWithFilterQuery: String =
    """
      |{
      |  "intervals" : {
      |    "my_text" : {
      |      "match" : {
      |        "query" : "hot porridge",
      |        "max_gaps" : 10,
      |        "filter" : {
      |          "not_containing" : {
      |            "match" : {
      |              "query" : "salty"
      |            }
      |          }
      |        }
      |      }
      |    }
      |  }
      |}
    """.stripMargin.replace("\n", "")

  test("Should correctly build intervals query with a script") {
    Given("An intervals query with a script")
    val query = IntervalsQuery(
      "my_text",
      Match("hot porridge").filter(
        IntervalsFilter().script(Script(
          "interval.start > 10 && interval.end < 20 && interval.gaps == 0"
        ))
      )
    )

    When("Intervals query is built")
    val queryBody = queries.IntervalsQueryBuilderFn(query)

    Then("query should have right fields")
    queryBody.string should matchJson(intervalsWithScriptQuery)
  }

  def intervalsWithScriptQuery: String =
    """
      |{
      |  "intervals" : {
      |    "my_text" : {
      |      "match" : {
      |        "query" : "hot porridge",
      |        "filter" : {
      |          "script" : {
      |            "source" : "interval.start > 10 && interval.end < 20 && interval.gaps == 0"
      |          }
      |        }
      |      }
      |    }
      |  }
      |}
    """.stripMargin.replace("\n", "")

  test("Should correctly build intervals query with a regex rule") {
    def expected =
      """
        |{
        |  "intervals": {
        |    "my_text": {
        |      "regexp": {
        |        "pattern": "*",
        |        "analyzer": "standard",
        |        "use_field": "my_text"
        |      }
        |    }
        |  }
        |}
      """.stripMargin
    Given("An intervals query with a regex rule")
    val query    =
      IntervalsQuery("my_text", Regexp(pattern = "*", analyzer = Some("standard"), useField = Some("my_text")))

    When("Intervals query is built")
    val queryBody = queries.IntervalsQueryBuilderFn(query)

    Then("query should have right fields")
    queryBody.string should matchJson(expected)
  }

  test("Should correctly build intervals query with a range rule") {
    def expected =
      """
        |{
        |  "intervals": {
        |    "my_text": {
        |      "range": {
        |        "gte": "a",
        |        "lte": "z",
        |        "analyzer": "standard",
        |        "use_field": "my_text"
        |      }
        |    }
        |  }
        |}
      """.stripMargin
    Given("An intervals query with a regex rule")
    val query    = IntervalsQuery(
      "my_text",
      Range(gte = Some("a"), lte = Some("z"), analyzer = Some("standard"), useField = Some("my_text"))
    )

    When("Intervals query is built")
    val queryBody = queries.IntervalsQueryBuilderFn(query)

    Then("query should have right fields")
    queryBody.string should matchJson(expected)
  }

  test("Should correctly build intervals boosted query") {
    Given("An intervals query with boost set")
    val query = IntervalsQuery(
      "my_text",
      AllOf(List(
        Match(query = "my favorite food").maxGaps(0).ordered(true),
        AnyOf(intervals =
          List(
            Match(query = "hot water"),
            Match(query = "cold porridge")
          )
        )
      )).ordered(true),
      Some(2.5D)
    )

    When("Intervals query is built")
    val queryBody = IntervalsQueryBuilderFn(query)

    Then("query should have right fields and boost set")
    queryBody.string should matchJson(intervalsBoostedQuery)
  }

  def intervalsBoostedQuery: String =
    """
      |{
      |  "intervals" : {
      |    "my_text" : {
      |      "boost": 2.5,
      |      "all_of" : {
      |        "ordered" : true,
      |        "intervals" : [
      |          {
      |            "match" : {
      |              "query" : "my favorite food",
      |              "max_gaps" : 0,
      |              "ordered" : true
      |            }
      |          },
      |          {
      |            "any_of" : {
      |              "intervals" : [
      |                { "match" : { "query" : "hot water" } },
      |                { "match" : { "query" : "cold porridge" } }
      |              ]
      |            }
      |          }
      |        ]
      |      }
      |    }
      |  }
      |}
    """.stripMargin.replace("\n", "")
}
