package edp.rider.rest.persistence.dal

import edp.rider.module.DbModule.db
import edp.rider.rest.persistence.base.BaseDalImpl
import edp.rider.rest.persistence.entities.{FeedbackFlowErr, FeedbackFlowErrTable}
import edp.rider.rest.util.CommonUtils.{maxTimeOut, minTimeOut}
import slick.lifted.TableQuery

import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class FeedbackFlowErrDal(feedbackFlowErrTable: TableQuery[FeedbackFlowErrTable])
  extends BaseDalImpl[FeedbackFlowErrTable, FeedbackFlowErr](feedbackFlowErrTable){
  def getSinkErrorMaxWatermark(streamId: Long, sourceNs: String, sinkNs: String): Future[Option[String]] = {
    super.findByFilter(str => str.streamId === streamId && str.sourceNamespace === sourceNs && str.sinkNamespace === sinkNs)
      .map[Option[String]](seq =>
      if (seq.isEmpty) None
      else Some(seq.map(_.errorMaxWaterMarkTs).max))
  }

  def getSinkErrorMinWatermark(streamId: Long, sourceNs: String, sinkNs: String): Future[Option[String]] = {
    super.findByFilter(str => str.streamId === streamId && str.sourceNamespace === sourceNs && str.sinkNamespace === sinkNs)
      .map[Option[String]](seq =>
      if (seq.isEmpty) None
      else Some(seq.map(_.errorMinWaterMarkTs).min))
  }

  def getSinkErrorCount(streamId: Long, sourceNs: String, sinkNs: String): Future[Option[Long]] = {
    super.findByFilter(str => str.streamId === streamId && str.sourceNamespace === sourceNs && str.sinkNamespace === sinkNs)
      .map[Option[Long]](seq =>
      if (seq.isEmpty) None
      else Some(seq.map(_.errorCount).sum))
  }

  def deleteHistory(pastNdays: String) = {
    val deleteMaxId = Await.result(
      db.run(feedbackFlowErrTable.withFilter(_.feedbackTime <= pastNdays).map(_.id).max.result).mapTo[Option[Long]], minTimeOut)
    if (deleteMaxId.nonEmpty) Await.result(super.deleteByFilter(_.id <= deleteMaxId), maxTimeOut)
  }
}
