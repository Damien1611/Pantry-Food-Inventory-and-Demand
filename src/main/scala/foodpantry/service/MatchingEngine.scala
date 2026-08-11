package foodpantry.service

import foodpantry.model._
import java.time.LocalDate

// ai-assisted: #4
// why: Implement the allocation algorithm using recursion to guarantee immutability (S1-11) and optimize waste-minimisation based on food expiration date.

// ── Optimization Matching Engine ─────────────────────────────────────────────
// MatchingEngine matches family requests to available inventory items.
// It uses a greedy allocation approach implemented tail-recursively.
object MatchingEngine {

  // S1-13: DRY - Extract matching and sorting logic into a reusable logic component.
  // Matches requests to inventory. It prioritizes:
  // 1. High urgency requests first.
  // 2. Perishable items that expire first (waste minimization).
  // 3. Dietary restrictions matching.
  def generatePlan(
    inventory: List[PantryItem],
    requests: List[FamilyRequest]
  ): (List[DistributionPlan], List[PantryItem]) = {

    // ── Request Sorting ──────────────────────────────────────────────────────
    // Sort requests by urgency (descending) and household size (descending).
    // Urgent, larger families are processed first to ensure critical needs are met.
    val sortedRequests = requests.sortBy { req =>
      (-req.urgencyLevel, -req.householdSize)
    }

    // ── Recursive Allocation Loop ─────────────────────────────────────────────
    // S1-11: Immutability - We use recursion instead of mutable state loops or mutable arrays.
    // Navigates through each sorted family request one by one.
    def allocateRecursive(
      remainingRequests: List[FamilyRequest],
      currentInventory: List[PantryItem],
      accPlans: List[DistributionPlan]
    ): (List[DistributionPlan], List[PantryItem]) = {
      remainingRequests match {
        case Nil =>
          (accPlans.reverse, currentInventory)
        case currentRequest :: tailRequests =>
          val requestedCategory = currentRequest.requestCategory
          val householdSizeVal  = currentRequest.householdSize
          // A family receives up to 2.0 units/kg of items per household size member
          val targetQuantity: Double = householdSizeVal.toDouble * 2.0

          val today = LocalDate.now()
          
          // ── Inventory Filtering ──────────────────────────────────────────────
          // Filter candidate items in current inventory:
          // 1. Matches category and quantity > 0.0
          // 2. Exclude already-expired PerishableFood items (expirationDate is before today)
          val candidates = currentInventory.filter { item =>
            item.category.equalsIgnoreCase(requestedCategory) && item.quantity > 0.0 && (item match {
              case perishable: PerishableFood => !perishable.expirationDate.isBefore(today)
              case _: ShelfStableFood         => true
            })
          }

          // ── Dietary and Expiry Sorting ───────────────────────────────────────
          // Sort candidates:
          // If PerishableFood, sort by expiration date ascending (earliest first, to minimize waste)
          // Also filter food items by dietary restriction compatibility:
          // - A Standard request can be satisfied by any item (any tag is fine).
          // - A non-Standard request can only be satisfied by an item whose dietaryTag matches exactly.
          val sortedCandidates = candidates.filter { item =>
            val tag     = currentRequest.dietaryRestriction.trim.toLowerCase
            val itemTag = item.dietaryTag.trim.toLowerCase
            if (tag == "standard") true
            else itemTag == tag
          }.sortBy {
            case perishable: PerishableFood =>
              perishable.expirationDate.toEpochDay
            case _: ShelfStableFood =>
              Long.MaxValue // Shelf-stable items do not expire and are checked last
          }

          // ── Candidate Allocation ─────────────────────────────────────────────
          // Greedily allocate from sorted candidates up to the needed amount
          def allocateFromCandidates(
            needed: Double,
            availCandidates: List[PantryItem],
            allocatedAcc: List[Allocation],
            inventoryState: List[PantryItem]
          ): (List[Allocation], List[PantryItem]) = {
            if (needed <= 0.0 || availCandidates.isEmpty) {
              (allocatedAcc.reverse, inventoryState)
            } else {
              val candidate = availCandidates.head
              val takeQty   = Math.min(needed, candidate.quantity)
              if (takeQty > 0.0) {
                // Update candidate quantity in inventory state
                val updatedInventoryState = inventoryState.map { item =>
                  if (item.itemId == candidate.itemId) {
                    item match {
                      case perishable: PerishableFood   => perishable.copy(quantity = perishable.quantity - takeQty)
                      case shelfStable: ShelfStableFood => shelfStable.copy(quantity = shelfStable.quantity - takeQty)
                    }
                  } else {
                    item
                  }
                }
                val newAllocation = Allocation(candidate, takeQty)
                allocateFromCandidates(
                  needed - takeQty,
                  availCandidates.tail,
                  newAllocation :: allocatedAcc,
                  updatedInventoryState
                )
              } else {
                allocateFromCandidates(needed, availCandidates.tail, allocatedAcc, inventoryState)
              }
            }
          }

          // ── Plan Assembly ────────────────────────────────────────────────────
          // Run the greedy allocation and build the DistributionPlan.
          val (allocations, updatedInventory) = allocateFromCandidates(
            targetQuantity,
            sortedCandidates,
            Nil,
            currentInventory
          )

          val newPlan = DistributionPlan(
            planId      = s"PLAN-${currentRequest.requestId}",
            planDate    = LocalDate.now(),
            request     = currentRequest,
            allocations = allocations
          )

          allocateRecursive(tailRequests, updatedInventory, newPlan :: accPlans)
      }
    }

    allocateRecursive(sortedRequests, inventory, Nil)
  }
}
