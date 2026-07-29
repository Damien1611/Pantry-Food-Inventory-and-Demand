package foodpantry

import java.time.LocalDate
import scala.util.Try

// ai-assisted: #2
// why: Implement a generic serializer trait supporting parametric polymorphism (S1-9) to standardise CSV record loading.
trait RecordSerializer[T] {
  def serialize(record: T): String
  def deserialize(line: String): Try[T]
}

// S1-13: DRY - Helper object extracting shared parsing logic to avoid copy-paste
object SerializationHelper {
  // S1-12: Exception handling - wrapping parsing functions in scala.util.Try
  def safeParseInt(text: String): Try[Int] = {
    Try(text.trim.toInt)
  }

  // S1-12: Exception handling - wrapping parsing functions in scala.util.Try
  def safeParseDouble(text: String): Try[Double] = {
    Try(text.trim.toDouble)
  }

  // S1-12: Exception handling - wrapping parsing functions in scala.util.Try
  def safeParseDate(text: String): Try[LocalDate] = {
    Try(LocalDate.parse(text.trim))
  }
}

object PerishableFoodSerializer extends RecordSerializer[PerishableFood] {
  override def serialize(item: PerishableFood): String = {
    s"Perishable,${item.itemId},${item.itemName},${item.quantity},${item.category},${item.expirationDate},${item.storageTemp},${item.dietaryTag},${item.unitType}"
  }

  override def deserialize(line: String): Try[PerishableFood] = {
    val parts = line.split(",")
    if (parts.length == 9 && parts(0) == "Perishable") {
      for {
        quantityVal <- SerializationHelper.safeParseDouble(parts(3))
        expiryDate <- SerializationHelper.safeParseDate(parts(5))
      } yield PerishableFood(parts(1).trim, parts(2).trim, quantityVal, parts(4).trim, expiryDate, parts(6).trim, parts(7).trim, parts(8).trim)
    } else {
      scala.util.Failure(new IllegalArgumentException("Invalid format for PerishableFood"))
    }
  }
}

object ShelfStableFoodSerializer extends RecordSerializer[ShelfStableFood] {
  override def serialize(item: ShelfStableFood): String = {
    s"ShelfStable,${item.itemId},${item.itemName},${item.quantity},${item.category},${item.storageTemp},${item.dietaryTag},${item.unitType}"
  }

  override def deserialize(line: String): Try[ShelfStableFood] = {
    val parts = line.split(",")
    if (parts.length == 8 && parts(0) == "ShelfStable") {
      SerializationHelper.safeParseDouble(parts(3)).map { quantityVal =>
        ShelfStableFood(parts(1).trim, parts(2).trim, quantityVal, parts(4).trim, parts(5).trim, parts(6).trim, parts(7).trim)
      }
    } else {
      scala.util.Failure(new IllegalArgumentException("Invalid format for ShelfStableFood"))
    }
  }
}

object PantryItemSerializer extends RecordSerializer[PantryItem] {
  override def serialize(item: PantryItem): String = item match {
    case perishable: PerishableFood => PerishableFoodSerializer.serialize(perishable)
    case shelfStable: ShelfStableFood => ShelfStableFoodSerializer.serialize(shelfStable)
  }

  override def deserialize(line: String): Try[PantryItem] = {
    val parts = line.split(",")
    if (parts.length > 0) {
      parts(0) match {
        case "Perishable" => PerishableFoodSerializer.deserialize(line).map(item => item: PantryItem)
        case "ShelfStable" => ShelfStableFoodSerializer.deserialize(line).map(item => item: PantryItem)
        case other => scala.util.Failure(new IllegalArgumentException(s"Unknown prefix: $other"))
      }
    } else {
      scala.util.Failure(new IllegalArgumentException("Empty record line"))
    }
  }
}

object FamilyRequestSerializer extends RecordSerializer[FamilyRequest] {
  override def serialize(req: FamilyRequest): String = {
    s"${req.requestId},${req.familyName},${req.householdSize},${req.requestCategory},${req.dietaryRestriction},${req.urgencyLevel}"
  }

  override def deserialize(line: String): Try[FamilyRequest] = {
    val parts = line.split(",")
    if (parts.length == 6) {
      for {
        sizeVal <- SerializationHelper.safeParseInt(parts(2))
        urgencyVal <- SerializationHelper.safeParseInt(parts(5))
      } yield FamilyRequest(parts(0).trim, parts(1).trim, sizeVal, parts(3).trim, parts(4).trim, urgencyVal)
    } else {
      scala.util.Failure(new IllegalArgumentException("Invalid format for FamilyRequest"))
    }
  }
}
