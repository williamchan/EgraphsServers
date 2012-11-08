package egraphs.toybox

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result
import play.api.mvc.AnyContent

abstract class ToyBoxAuthenticator extends Controller {
    def login: Action[AnyContent]
    def authenticate: Action[AnyContent]
}