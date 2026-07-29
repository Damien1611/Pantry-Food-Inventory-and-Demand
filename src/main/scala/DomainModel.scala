package foodpantry

import java.time.LocalDate

// ai-assisted: #1
// why: Define the core sealed trait hierarchy for pantry items to support compile-time exhaustive checks and implement S1-7.
sealed trait PantryItem {
  val itemId: String
  val itemName: String
  val quantity: Double
  val category: String
  val storageTemp: String
  val dietaryTag: String
  val unitType: String

  // S1-8: Subtype polymorphism - abstract method to be explicitly overridden by subclasses
  def displayDetails: String
}

case class PerishableFood(
  override val itemId: String,
  override val itemName: String,
  override val quantity: Double,
  override val category: String,
  expirationDate: LocalDate,
  override val storageTemp: String,
  override val dietaryTag: String,
  override val unitType: String
) extends PantryItem {
  // S1-8: Explicit override of displayDetails
  override def displayDetails: String = {
    s"Perishable | Exp: $expirationDate | Storage: $storageTemp | Unit: $unitType | Tag: $dietaryTag"
  }
}

case class ShelfStableFood(
  override val itemId: String,
  override val itemName: String,
  override val quantity: Double,
  override val category: String,
  override val storageTemp: String,
  override val dietaryTag: String,
  override val unitType: String
) extends PantryItem {
  // S1-8: Explicit override of displayDetails
  override def displayDetails: String = {
    s"Shelf-Stable | Storage: $storageTemp | Unit: $unitType | Tag: $dietaryTag"
  }
}

case class FamilyRequest(
  requestId: String,
  familyName: String,
  householdSize: Int,
  requestCategory: String, // Maps to primary categories, e.g. "Proteins", "Grains & Cereals"
  dietaryRestriction: String, // "Standard", "Vegetarian", "Gluten-Free", "Halal"
  urgencyLevel: Int // 1 to 5
)

case class Allocation(
  item: PantryItem,
  allocatedQuantity: Double
)

case class DistributionPlan(
  planId: String,
  planDate: LocalDate,
  request: FamilyRequest,
  allocations: List[Allocation]
)
