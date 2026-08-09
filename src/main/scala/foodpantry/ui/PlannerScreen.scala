package foodpantry.ui

import foodpantry.model._
import foodpantry.service.MatchingEngine
import java.time.LocalDate
import scala.util.{Success, Failure}
import scalafx.geometry.Insets
import scalafx.scene.control._
import scalafx.scene.layout._

// SCREEN 4: Distribution Planner Screen
object PlannerScreen extends UIHelpers {

  def create(ctx: AppContext): Pane = {
    val titleLbl    = new Label("Distribution Planner & Waste Reducer") { styleClass.add("header-title") }
    val subtitleLbl = new Label("Run the optimization engine to match pending family requests to expiring inventory items.") { styleClass.add("header-subtitle") }

    // Using object-level remainingBuffer to persist state across screen navigation

    // ── TABLE 1: Allocation Plan per Family ───────────────────────────────────
    val planTable = new TableView[DistributionPlan](ctx.planBuffer) {
      prefHeight = 230
      placeholder = new Label("Click 'Run Plan Optimization' to generate the allocation plan.") {
        style = "-fx-text-fill: #64748b;"
      }
    }

    val colFamily   = strColumn[DistributionPlan]("Family Name", 150)(_.request.familyName)
    val colSize     = strColumn[DistributionPlan]("Size", 55)(_.request.householdSize.toString)
    val colUrgency  = strColumn[DistributionPlan]("Urgency", 65)(_.request.urgencyLevel.toString)
    val colCategory = strColumn[DistributionPlan]("Category", 170)(_.request.requestCategory)
    val colDiet     = strColumn[DistributionPlan]("Dietary", 100)(_.request.dietaryRestriction)
    val colPlanDate = strColumn[DistributionPlan]("Plan Date", 100)(_.planDate.toString)
    val colAllocated = strColumn[DistributionPlan]("Allocated Items", 320) { plan =>
      if (plan.allocations.isEmpty) "*** NO MATCHING INVENTORY AVAILABLE ***"
      else plan.allocations.map(alloc => f"${alloc.allocatedQuantity}%.2f ${alloc.item.unitType} of ${alloc.item.itemName}").mkString(" | ")
    }

    planTable.columns ++= Seq(colFamily, colSize, colUrgency, colCategory, colDiet, colPlanDate, colAllocated)

    // ── TABLE 2: Remaining Inventory After Allocation ─────────────────────────
    val remainingLabel = new Label("Projected Remaining Inventory After Allocation") {
      style = "-fx-text-fill: #f8fafc; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 0 4px 0;"
    }

    val remainingTable = new TableView[PantryItem](ctx.remainingBuffer) {
      prefHeight = 200
      placeholder = new Label("Run the planner to see remaining stock.") {
        style = "-fx-text-fill: #64748b;"
      }
    }

    val rColName     = strColumn[PantryItem]("Item Name", 170)(_.itemName)
    val rColCategory = strColumn[PantryItem]("Category", 175)(_.category)
    val rColQty      = strColumn[PantryItem]("Qty Remaining", 120)(item => f"${item.quantity}%.2f")
    val rColUnit     = strColumn[PantryItem]("Unit", 65)(_.unitType)
    val rColStorage  = strColumn[PantryItem]("Storage", 110)(_.storageTemp)
    val rColTag      = strColumn[PantryItem]("Dietary Tag", 110)(_.dietaryTag)

    remainingTable.columns ++= Seq(rColName, rColCategory, rColQty, rColUnit, rColStorage, rColTag)

    // ── Buttons ───────────────────────────────────────────────────────────────
    val btnRun = new Button("Run Plan Optimization") {
      styleClass.add("btn-primary")
    }

    val btnSavePlan = new Button("Export Distribution Plan") {
      styleClass.add("btn-primary")
      disable = ctx.planBuffer.isEmpty
    }

    btnRun.onAction = ctx.handle {
      val inventoryItems = ctx.inventoryBuffer.toList
      val familyRequests = ctx.requestsBuffer.toList

      if (inventoryItems.isEmpty) {
        ctx.statusMessage.value = "Cannot run planner: Inventory is completely empty."
      } else if (familyRequests.isEmpty) {
        ctx.statusMessage.value = "Cannot run planner: No pending family requests found."
      } else {
        val (generatedPlans, remaining) = MatchingEngine.generatePlan(inventoryItems, familyRequests)

        // Populate plan table
        ctx.planBuffer.clear()
        ctx.planBuffer.addAll(generatedPlans)

        // Populate remaining inventory table (only items with stock left)
        ctx.remainingBuffer.clear()
        ctx.remainingBuffer.addAll(remaining.filter(_.quantity > 0.0))

        btnSavePlan.disable = false
        ctx.statusMessage.value = s"Plan generated: ${generatedPlans.size} families processed, ${remaining.count(_.quantity > 0.0)} items remaining in stock."
      }
    }

    // ai-assisted: #10
    // why: Export distribution plan and remaining stock report to file using PrintWriter wrapped in a safe scala.util.Try block with finally resource cleanup.
    btnSavePlan.onAction = ctx.handle {
      val exportPath = "src/main/resources/distribution_plan_report.txt"
      scala.util.Try {
        val pw = new java.io.PrintWriter(new java.io.File(exportPath), "UTF-8")
        try {
          pw.println(s"DAILY DISTRIBUTION PLAN — ${LocalDate.now()}")
          pw.println("=" * 72)
          ctx.planBuffer.foreach { plan =>
            pw.println(s"Family: ${plan.request.familyName} | Size: ${plan.request.householdSize} | Urgency: ${plan.request.urgencyLevel}")
            pw.println(s"  Category: ${plan.request.requestCategory} | Dietary: ${plan.request.dietaryRestriction}")
            if (plan.allocations.isEmpty) {
              pw.println("  -> *** NO MATCHING INVENTORY AVAILABLE ***")
            } else {
              plan.allocations.foreach { alloc =>
                pw.println(f"  -> ${alloc.allocatedQuantity}%.2f ${alloc.item.unitType} of ${alloc.item.itemName}")
              }
            }
            pw.println("-" * 72)
          }
          pw.println("\nREMAINING INVENTORY AFTER ALLOCATION:")
          pw.println("=" * 72)
          ctx.remainingBuffer.foreach { item =>
            pw.println(f"  ${item.itemName.padTo(25, ' ')} | ${item.category.padTo(22, ' ')} | ${item.quantity}%.2f ${item.unitType}")
          }
        } finally {
          pw.close()
        }
      } match {
        case Success(_) =>
          ctx.statusMessage.value = "Success: Distribution plan saved to src/main/resources/distribution_plan_report.txt"
        case Failure(errorEx) =>
          ctx.statusMessage.value = s"Failed to export report: ${errorEx.getMessage}"
      }
    }

    val actionRow = new HBox {
      spacing  = 15
      children = Seq(btnRun, btnSavePlan)
    }

    new VBox {
      padding  = Insets(24)
      spacing  = 12
      children = Seq(titleLbl, subtitleLbl, actionRow, planTable, remainingLabel, remainingTable)
    }
  }
}
