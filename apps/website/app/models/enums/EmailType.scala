package models.enums

import services.Utils
import egraphs.playutils.Enum

object EmailType extends Enum {
  sealed trait EnumVal extends Value  

  val AccountConfirmation = new EnumVal {
    val name = "Account Confirmation"
    val subject = "Welcome to Egraphs!"
  }  

  val AccountVerification = new EnumVal {
    val name = "Account Verification"
    val subject = "Welcome to Egraphs!"      
  }

  val CelebrityWelcome = new EnumVal {
    val name = "Celebrity Welcome"
    val subject = "Welcome to Egraphs!"
  }

  val OrderConfirmation = new EnumVal {
    val name = "Order Confirmation"
    val subject = "Order Confirmation"
  }

  val ResetPassword = new EnumVal {
    val name = "Reset Password"
    val subject = "Egraphs Password Recovery"
  }
  
  val ViewEgraph = new EnumVal {
    val name = "View Egraph"
    val subject = "I just finished signing your egraph"
  }

  val ViewGiftEgraph = new EnumVal {
    val name = "View Gift Egraph"
    val subject = "I just finished signing your egraph"
  }
}

trait HasEmailType[T] {
  def _emailType: String
  
  def emailType: EmailType.EnumVal = {
    EmailType(_emailType).getOrElse(
      throw new IllegalArgumentException(_emailType)
    )
  }
  
  def withEmailType(emailType: EmailType.EnumVal): T
}