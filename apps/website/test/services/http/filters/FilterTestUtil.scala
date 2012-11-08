package services.http.filters

import play.api.mvc.Results.Ok
import play.api.mvc.Result
import play.api.data.Form

object FilterTestUtil {
  class RichEitherErrorOrSuccess(errorOrSuccess: Either[Result, _]) {
    // will give either the error result it already has or Ok otherwise.
    def toErrorOrOkResult: Result = {
      errorOrSuccess.fold(
        error => error,
        _ => Ok)
    }
  }

  implicit def EitherErrorOrSuccess2RichErrorOrSuccess(errorOrSuccess: Either[Result, _]) =
    new RichEitherErrorOrSuccess(errorOrSuccess)
}