package models.frontend.marketplace

import egraphs.playutils.Enum

object CelebritySortingTypes extends Enum {
  sealed trait EnumVal extends Value {
    def displayName: String
  }
  // val RecentlyAdded = new EnumVal {
  //   val name = "RecentlyAdded"
  //   val displayName ="Recently Added" 
  // }
  // val MostPopular = new EnumVal {
  //   val name = "MostPopular"
  //   val displayName ="Most Popular"
  // }
  val MostRelevant = new EnumVal {
    val name = "MostRelevant"
    val displayName ="Most Relevant"
  }

  val PriceAscending = new EnumVal {
    val name = "PriceAscending"
    val displayName ="Price (Low to High)"
  }
  val PriceDecending = new EnumVal {
    val name = "PriceDecending"
    val displayName ="Price (High to Low)"
  }
  val Alphabetical = new EnumVal {
    val name = "Alphabetical"
    val displayName ="Alphabetical (A-Z)"
  }
  // val ReverseAlphabetical = new EnumVal {
  //   val name = "ReverseAlphabetical"
  //   val displayName ="Alphabetical (Z-A)"
  // }
}
