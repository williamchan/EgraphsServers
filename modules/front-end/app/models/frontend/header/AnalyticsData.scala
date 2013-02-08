package models.frontend.header

/**
 * Data needed by analytics for it to do it's job.
 */
//TODO: Hook this up instead of using HeaderData
case class AnalyticsData (
  updateMixpanelAlias: Boolean = false
)