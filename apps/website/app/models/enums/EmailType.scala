package models.enums

import services.Utils
import egraphs.playutils.Enum

object EmailType extends Enum {
  sealed trait EnumVal extends Value  

  val AccountConfirmation = new EnumVal {
    val name = "Account Confirmation"
  }  

  val AccountVerification = new EnumVal {
    val name = "Account Verification"
  }

  val CelebrityRequest = new EnumVal {
    val name = "Celebrity Request"
  }

  val CelebrityWelcome = new EnumVal {
    val name = "Celebrity Welcome"
  }

  val EnrollmentComplete = new EnumVal {
    val name = "Enrollment Complete"
  }

  val OrderConfirmation = new EnumVal {
    val name = "Order Confirmation"
  }

  val ResetPassword = new EnumVal {
    val name = "Reset Password"
  }
  
  val ViewEgraph = new EnumVal {
    val name = "View Egraph"
  }

  val ViewGiftEgraph = new EnumVal {
    val name = "View Gift Egraph"
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