package services

import scala.concurrent.Future

trait MessageProcessor[T, R] {
  def process(msg: T):Future[R]
}
