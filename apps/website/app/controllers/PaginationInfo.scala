package controllers

import services.Utils
import models.frontend.PaginationInfo

object PaginationInfoFactory {
  def create[A](
    pagedQuery: (Iterable[A], Int, Option[Int]),
    pageLength: Int = Utils.defaultPageLength,
    baseUrl: String,
    filter: Option[String] = None)
  : PaginationInfo = {

    val curPage = pagedQuery._2
    val totalResults = pagedQuery._3

    val showPaging = totalResults.isDefined && totalResults.get > pageLength
    val totalResultsStr = if (totalResults.isDefined) ("- " + totalResults.get + " results") else ""

    val (firstUrl, prevUrl, nextUrl, lastUrl) = if (showPaging) {
      val showFirst: Boolean = curPage > 2
      val firstUrl = if (showFirst) Some(withPageQuery(baseUrl, 1, filter)) else None

      val showPrev: Boolean = curPage > 1
      val prevUrl = if (showPrev) Some(withPageQuery(baseUrl, curPage - 1, filter)) else None

      val totalNumPages = if (totalResults.get % pageLength > 0) {
        totalResults.get / pageLength + 1
      } else {
        totalResults.get / pageLength
      }

      val showNext: Boolean = curPage < totalNumPages
      val nextUrl = if (showNext) Some(withPageQuery(baseUrl, curPage + 1, filter)) else None

      val showLast: Boolean = curPage < totalNumPages
      val lastUrl = if (showLast) Some(withPageQuery(baseUrl, totalNumPages, filter)) else None
      (firstUrl, prevUrl, nextUrl, lastUrl)
    } else { (None, None, None, None) }
    PaginationInfo(showPaging = showPaging, totalResultsStr = totalResultsStr, firstUrl = firstUrl, prevUrl = prevUrl, nextUrl = nextUrl, lastUrl = lastUrl)
  }
  
  private def withPageQuery(url: String,
                            page: Int,
                            filter: Option[String]): String = {
    val filterStr = filter match {
      case Some(f) => "&filter=" + f
      case None => ""
    }
    if (url.contains('?')) {
      url + "page=" + page + filterStr
    } else {
      url + "?page=" + page + filterStr
    }
  }
}