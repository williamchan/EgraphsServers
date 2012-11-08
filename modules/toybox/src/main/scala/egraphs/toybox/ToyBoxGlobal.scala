package egraphs.toybox

import play.api.Application
import play.api.GlobalSettings
import play.api.mvc.Handler
import play.api.mvc.RequestHeader

object ToyBoxGlobal extends GlobalSettings {
    // Either grab the factory from the config or have the user modify the source?
    val toyBox = ToyBox()   // temporary

    override def onStart(app: Application) {
        // print settings?
    }

    override def onRouteRequest(req: RequestHeader): Option[Handler] = {
        toyBox.serviceRouteRequest(req)
    }
}