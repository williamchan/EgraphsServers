package services.http

import play.mvc.Http.Request
import models._
import com.google.inject.Inject
import play.mvc.results.Forbidden

class AdminRequestFilters @Inject()(celebStore: CelebrityStore,
                                    accountFilters: AccountRequestFilters,
                                    productFilters: ProductQueryFilters) {

  import OptionParams.Conversions._

  def requireCelebrity(continue: (Celebrity) => Any)(implicit request: Request) = {
    request.params.getOption("celebrityId") match {
      case None =>
        new Forbidden("Valid celebrity ID was required but not provided")

      case Some(celebrityId) => {
        celebStore.findById(celebrityId.toLong) match {
          case None =>
            new Forbidden("No celebrity was found with ID " + celebrityId)

          case Some(celebrity) => {
            continue(celebrity)
          }
        }
      }
    }
  }

}
