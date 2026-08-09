package foodpantry.ui

import foodpantry.model._
import java.time.LocalDate
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control._
import scalafx.scene.layout._

// SCREEN 1: Dashboard Overview Screen
object DashboardScreen extends UIHelpers {

  def create(ctx: AppContext): Pane = {
    val titleLbl    = new Label("Dashboard Overview") { styleClass.add("header-title") }
    val subtitleLbl = new Label("Real-time summary of inventory, urgent demands, and waste risks.") { styleClass.add("header-subtitle") }

    // Summary statistics computations (separated by unit types: kg vs units)
    val totalWeight = ctx.inventoryBuffer.filter(_.unitType == "kg").map(_.quantity).sum
    val totalUnits  = ctx.inventoryBuffer.filter(_.unitType == "units").map(_.quantity).sum

    val todayDate    = LocalDate.now()
    val expiringCount = ctx.inventoryBuffer.collect {
      case perishable: PerishableFood if perishable.expirationDate.isBefore(todayDate.plusDays(7)) && perishable.expirationDate.isAfter(todayDate.minusDays(1)) =>
        perishable.quantity
    }.sum

    val expiredCount = ctx.inventoryBuffer.collect {
      case perishable: PerishableFood if perishable.expirationDate.isBefore(todayDate) =>
        perishable.quantity
    }.sum

    val activeRequestsCount      = ctx.requestsBuffer.size
    val highlyUrgentRequestsCount = ctx.requestsBuffer.count(req => req.urgencyLevel >= 4)

    // Layout cards for metrics
    val cardWeightStock  = metricCard("Weight-Based Stock", f"$totalWeight%.2f kg")()
    val cardUnitStock    = metricCard("Count-Based Stock", f"$totalUnits%.0f units")()
    val cardExpiringSoon = metricCard("Expiring Soon (<7 Days)", f"$expiringCount%.1f items") { lbl =>
      if (expiringCount > 0.0) lbl.styleClass.add("metric-warning")
    }
    val cardExpired  = metricCard("Expired (Waste Risk)", f"$expiredCount%.1f items") { lbl =>
      if (expiredCount > 0.0) lbl.style = "-fx-text-fill: #ef4444;"
    }
    val cardRequests = metricCard("Pending Requests", s"$activeRequestsCount families")()
    val cardUrgent   = metricCard("Highly Urgent (Level 4+)", s"$highlyUrgentRequestsCount families") { lbl =>
      if (highlyUrgentRequestsCount > 0) lbl.style = "-fx-text-fill: #ef4444;"
    }

    val flowPane = new FlowPane {
      hgap    = 12
      vgap    = 12
      padding = Insets(10, 0, 20, 0)
      children = Seq(cardWeightStock, cardUnitStock, cardExpiringSoon, cardExpired, cardRequests, cardUrgent)
    }

    // Table of high-risk items (expired or expiring soon)
    val riskItemsLabel = new Label("High-Risk Perishable Food Items") {
      style = "-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 10px 0 5px 0;"
    }

    val riskItems = ctx.inventoryBuffer.collect {
      case p: PerishableFood if p.expirationDate.isBefore(todayDate.plusDays(7)) => p
    }.toSeq.sortBy(_.expirationDate.toEpochDay)

    val riskTable = new TableView[PerishableFood](ObservableBuffer.from(riskItems)) {
      prefHeight = 220
      placeholder = new Label("No expired or expiring items. Excellent inventory health!") {
        style = "-fx-text-fill: #64748b;"
      }
    }

    val rColStatus   = strColumn[PerishableFood]("Status", 130)(item => if (item.expirationDate.isBefore(todayDate)) "⚠ EXPIRED" else "⏳ EXPIRING SOON")
    val rColName     = strColumn[PerishableFood]("Item Name", 160)(_.itemName)
    val rColCategory = strColumn[PerishableFood]("Category", 220)(_.category)
    val rColQty      = strColumn[PerishableFood]("Quantity", 110)(item => f"${item.quantity}%.2f ${item.unitType}")
    val rColExpiry   = strColumn[PerishableFood]("Expiry Date", 110)(_.expirationDate.toString)
    val rColStorage  = strColumn[PerishableFood]("Storage", 110)(_.storageTemp)

    riskTable.columns ++= Seq(rColStatus, rColName, rColCategory, rColQty, rColExpiry, rColStorage)

    val contentVBox = new VBox {
      padding  = Insets(24)
      spacing  = 10
      children = Seq(titleLbl, subtitleLbl, flowPane, riskItemsLabel, riskTable)
    }
    VBox.setVgrow(riskTable, Priority.Always)
    contentVBox
  }
}
