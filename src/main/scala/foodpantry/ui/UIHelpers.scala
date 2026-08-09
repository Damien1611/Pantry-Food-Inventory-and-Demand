package foodpantry.ui

import scalafx.Includes._
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.beans.property.StringProperty

// Shared UI utilities used by all four screen objects.
// Screens extend this trait to access helpers without duplication.
trait UIHelpers {

  // List of primary food categories — shared across Inventory and Requests forms
  val foodCategories: Seq[String] = Seq(
    "Grains & Cereals",
    "Proteins",
    "Fruits & Vegetables",
    "Canned/Prepared Meals",
    "Condiments & Cooking Essentials",
    "Beverages",
    "Baby & Infant Items",
    "Non-Food Essentials"
  )

  // Generic helper for creating standard TableColumn instances
  def strColumn[T](header: String, colWidth: Double)(extractor: T => String): TableColumn[T, String] = {
    new TableColumn[T, String](header) {
      cellValueFactory = { cd => StringProperty(extractor(cd.value)) }
      prefWidth = colWidth
    }
  }

  // Helper for building metric cards on the Dashboard
  def metricCard(titleText: String, valueText: String)(highlight: Label => Unit = _ => ()): VBox = {
    val valueLbl = new Label(valueText) { styleClass.add("metric-value") }
    highlight(valueLbl)
    new VBox {
      styleClass.add("metric-card")
      children = Seq(
        new Label(titleText) { styleClass.add("metric-title") },
        valueLbl
      )
    }
  }

  // ai-assisted: #9
  // why: Intercept Enter key presses on form TextFields using ScalaFX native KeyCode and KeyEvents to trigger submission (S2-10).
  def submitOnEnter(fields: TextField*)(action: => Unit): Unit = {
    val enterKeyHandler = (keyEvent: KeyEvent) => {
      if (keyEvent.code == KeyCode.Enter) {
        action
      }
    }
    fields.foreach(_.onKeyPressed = enterKeyHandler)
  }
}
