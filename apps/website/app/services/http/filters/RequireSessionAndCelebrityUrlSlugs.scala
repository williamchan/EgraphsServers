package services.http.filters

import com.google.inject.Inject
import play.api.mvc.{BodyParser, Action}
import play.api.mvc.BodyParsers.parse
import models.Celebrity


class RequireSessionAndCelebrityUrlSlugs @Inject() (
  requireCelebrityUrlSlug: RequireCelebrityUrlSlug,
  requireCelebrityPublishedAccess: RequireCelebrityPublishedAccess,
  requireSessionUrlSlug: RequireSessionUrlSlug,
  requireAdministratorLogin: RequireAdministratorLogin
) {

  def apply[T](
    sessionUrlSlug: String,
    celebrityUrlSlug: String,
    accessKey: String = "",
    parser: BodyParser[T] = parse.anyContent
  )(actionFactory: (String, Celebrity) => Action[T]): Action[T] = {
    requireCelebrityUrlSlug(celebrityUrlSlug, parser) { celebFromId =>
      def celebrityPublishedFilter = requireCelebrityPublishedAccess.filter((celebFromId, accessKey))
      requireAdministratorLogin.inSessionOrUseOtherFilter(celebFromId, parser)(otherFilter = celebrityPublishedFilter) { celebrity =>
        requireSessionUrlSlug(sessionUrlSlug) { sessionId =>
          Action[T](parser) { request =>
            actionFactory(sessionId, celebrity).apply(request)
          }
        }
      }
    }
  }
}
