package services.http.filters

import com.google.inject.Inject
import play.api.mvc.{BodyParser, Action}
import play.api.mvc.BodyParsers.parse
import models.Celebrity


class RequireSessionAndCelebrityUrlSlugs @Inject() (
  requireCelebrityId: RequireCelebrityId,
  requireCelebrityPublishedAccess: RequireCelebrityPublishedAccess,
  requireSessionUrlSlug: RequireSessionUrlSlug,
  requireAdministratorLogin: RequireAdministratorLogin
) {

  def apply[T](
    sessionId: String,
    celebrityId: Long,
    accessKey: String = "",
    parser: BodyParser[T] = parse.anyContent
  )(actionFactory: (String, Celebrity) => Action[T]): Action[T] = {
    requireCelebrityId(celebrityId, parser) { celebFromId =>
      def celebrityPublishedFilter = requireCelebrityPublishedAccess.filter((celebFromId, accessKey))
      requireAdministratorLogin.inSessionOrUseOtherFilter(celebFromId, parser)(otherFilter = celebrityPublishedFilter) { celebrity =>
        requireSessionUrlSlug(sessionId, parser) { sessionId =>
          Action[T](parser) { request =>
            actionFactory(sessionId, celebrity).apply(request)
          }
        }
      }
    }
  }
}
