package services.print

object PrintManufacturingInfo {

  /**
   * @return comma-separated values with header values as required by printing partner
   */
  def headerCSVLine: String = {
    List(
      "email_address",
      "name_full",
      "address1",
      "address2",
      "city",
      "state",
      "zip",
      "country",
      "phone",
      "item_id",
      "photo_id",
      "partner_photo_file",
      "qty",
      "line1",
      "line2",
      "line3",
      "bib_number"
    ).mkString(",")
  }

  /**
   * @param buyerEmail buyer's email
   * @param shippingAddress shipping address, ideally with commas between each address field
   * @param partnerPhotoFile filename of JPG to print
   * @return comma-separated values that should match up with the headers
   */
  def toCSVLine(buyerEmail: String,
                shippingAddress: String,
                partnerPhotoFile: String): String = {
    List(
      buyerEmail,       /*email_address*/
      shippingAddress,  /*in lieu of name_full and discrete address fields*/
      "",               /*country*/
      "313-334-7274",   /*phone*/
      "13893654",       /*item_id*/
      "",               /*photo_id*/
      partnerPhotoFile, /*partner_photo_file*/
      "1",              /*qty*/
      "",               /*line1*/
      "",               /*line2*/
      "",               /*line3*/
      ""                /*bib_number*/
    ).mkString(",")
  }
}