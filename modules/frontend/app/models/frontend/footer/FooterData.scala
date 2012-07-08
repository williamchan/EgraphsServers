package models.frontend.footer

import services.mvc.ImplicitHeaderAndFooterData

/**
 * Data needed to fgormat the footer
 *
 * @param aboutUsLink link to the "about us" page
 * @param faqLink link to the "faq" page
 * @param termsOfUseLink link to the "terms of use" page
 * @param privacyPolicyLink link to the "privacy policy" page
 * @param egraphsTwitterLink link to the Egraphs twitter page
 * @param egraphsFacebookLink link to the Egraphs facebook page
 */
case class FooterData(
  aboutUsLink: String="/about",
  faqLink: String="/faq",
  termsOfUseLink: String="/terms-of-use",
  privacyPolicyLink: String="/privacy-policy",
  egraphsTwitterLink: String=ImplicitHeaderAndFooterData.twitterLink,
  egraphsFacebookLink: String=ImplicitHeaderAndFooterData.facebookLink
)
