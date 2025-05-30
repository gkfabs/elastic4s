package com.sksamuel.elastic4s.monix

import com.sksamuel.elastic4s.{ElasticRequest, Executor, HttpClient, HttpResponse}
import monix.eval.Task

class TaskExecutor extends Executor[Task] {
  override def exec(client: HttpClient, request: ElasticRequest): Task[HttpResponse] =
    Task.async(k => client.send(request, k))
}
