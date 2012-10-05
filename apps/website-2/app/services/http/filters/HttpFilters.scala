package services.http.filters

import com.google.inject.Inject

// TODO: PLAY20 migration. Comment this.
class HttpFilters @Inject()(
  val requireAccountEmail: RequireAccountEmail,
  val requireAdministratorLogin: RequireAdministratorLogin,
  val requireAuthenticatedAccount: RequireAuthenticatedAccount,
  val requireAuthenticityTokenFilter: RequireAuthenticityTokenFilter,
  val requireCelebrityId: RequireCelebrityId,
  val requireCelebrityUrlSlug: RequireCelebrityUrlSlug,
  val requireCustomerId: RequireCustomerId,
  val requireCustomerLogin: RequireCustomerLogin,
  val requireCustomerUsername: RequireCustomerUsername,
  val requireEgraphId: RequireEgraphId,
  val requireOrderIdOfCelebrity: RequireOrderIdOfCelebrity,
  val requirePrintOrderId: RequirePrintOrderId,
  val requireProductUrlSlug: RequireProductUrlSlug,
  val requireCelebrityAndProductUrlSlugs: RequireCelebrityAndProductUrlSlugs,
  val requireResetPasswordSecret: RequireResetPasswordSecret,
  val requireSessionId: RequireSessionId
)
  
