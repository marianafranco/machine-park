package controllers

import play.api.mvc._

/**
 * Controller used to serve the html pages.
 */
object Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index("Machine Park"))
  }
}
