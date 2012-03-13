package controllers.website.admin

import play.data.validation._
import models._
import play.mvc.results.Redirect
import services.mail.Mail
import controllers.WebsiteControllers
import play.mvc.Controller
import services.Utils
import play.data.validation.Validation.ValidationResult
import play.{Play, Logger}
import services.blobs.Blobs.Conversions._

trait PostCelebrityEndpoint {
  this: Controller =>

  protected def mail: Mail

  protected def celebrityStore: CelebrityStore

  protected def accountStore: AccountStore

  /**
   * Creates a Celebrity. Follows this validation path:
   *
   * 1. Basic input validation.
   * 2. Search for Account by email and throw a validation error if it exists and either:
   *    a) an associated Celebrity already exists, or
   *    b) the provided password differs from the password stored on the Account,
   *    c) the provided password fails password validation.
   * 3. Search for a Celebrity with the url slug, and throw a validation error if one already exists.
   * 4. If no validation errors were encountered, create the Celebrity with a default Product.
   *
   * @return a redirect either to the Create Celebrity page with form errors or to
   *   the created Celebrity's page.
   */
  def postCelebrity(celebrityEmail: String,
                    celebrityPassword: String,
                    firstName: String,
                    lastName: String,
                    publicName: String,
                    description: String): Redirect = {
    import Validation.{required, email}

    // 1. Basic input validation.
    required("E-mail address", celebrityEmail)
    email("E-mail address", celebrityEmail)
    required("Password", celebrityPassword)
    required("Description", description)
    Validation.isTrue("Must provide either Public Name or First and Last Name", !publicName.isEmpty || (!firstName.isEmpty && !lastName.isEmpty))
    if (!validationErrors.isEmpty) {
      return WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityEndpoint.url())
    }

    // 2. Validate email and password.
    val account: Option[Account] = accountStore.findByEmail(celebrityEmail)
    if (account.isDefined) {
      val celebrityDoesNotAlreadyExist = account.get.celebrityId.isEmpty
      Validation.isTrue("Celebrity with e-mail address already exists", celebrityDoesNotAlreadyExist)
      if (celebrityDoesNotAlreadyExist && account.get.password.isDefined) {
        Validation.isTrue("A non-celebrity account with that e-mail already exists. Provide the correct password to turn this account into a celebrity account", account.get.password.get.is(celebrityPassword))
      }
    }
    val passwordValidationOrAccount: Either[ValidationResult, Account] = if (account.isDefined) {
      Right(account.get)
    } else {
      new Account(email = celebrityEmail).withPassword(celebrityPassword)
    }
    if (passwordValidationOrAccount.isLeft) {
      Validation.addError("Password", passwordValidationOrAccount.left.get.error.toString)
    }
    if (!validationErrors.isEmpty) {
      return WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityEndpoint.url())
    }

    // 3. Validate Celebrity is unique in urlSlug.
    val publicNameStr = if (publicName.isEmpty) firstName + " " + lastName else publicName
    val celebrity = new Celebrity(firstName = Utils.toOption(firstName),
      lastName = Utils.toOption(lastName),
      publicName = Utils.toOption(publicNameStr),
      description = Utils.toOption(description))
    val urlSlug = celebrity.urlSlug.get
    Validation.isTrue("Celebrity with same website name exists. Provide different public name", celebrityStore.findByUrlSlug(urlSlug).isEmpty)
    if (!validationErrors.isEmpty) {
      return WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityEndpoint.url())
    }

    // 4. Create Celebrity
    Logger.info("Creating celebrity")
    val savedCelebrity = celebrity.save()
    passwordValidationOrAccount.right.get.copy(celebrityId = Some(savedCelebrity.id)).save()

    savedCelebrity.newProduct.copy(
      priceInCurrency = 50,
      name = publicNameStr + "'s Egraph",
      description = "My First Available Egraph"
    ).save().withPhoto(Play.getFile("test/files/kapler/product-1.jpg")).save()

    // Send the order email
    //      val email = new SimpleEmail()
    //      email.setFrom("noreply@egraphs.com", "eGraphs")
    //      email.addTo(accountStore.findByCustomerId(buyer.id).get.email, buyerName)
    //      email.setSubject("Order Confirmation")
    //      email.setMsg(views.Application.html.order_confirmation_email(
    //        buyer, recipient, celebrity, product, chargedOrder
    //      ).toString().trim())
    //      mail.send(email)

    val celebrityUrlSlug = celebrity.urlSlug.get
    new Redirect(WebsiteControllers.lookupGetCelebrity(celebrityUrlSlug).url)
  }
}
