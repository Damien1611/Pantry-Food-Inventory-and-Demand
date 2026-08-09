package foodpantry.ui

import foodpantry.model._
import foodpantry.repo.DataRepository
import scalafx.application.JFXApp3
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scalafx.scene.control.{Alert, ButtonType}
import scala.util.{Success, Failure}

// AppContext carries all shared mutable state and helper methods.
// Passed into each screen's create() method so screens can read/write shared data
// without the screens needing to be inner members of Main.
class AppContext(
  val inventoryBuffer: ObservableBuffer[PantryItem],
  val requestsBuffer:  ObservableBuffer[FamilyRequest],
  val planBuffer:      ObservableBuffer[DistributionPlan],
  val remainingBuffer: ObservableBuffer[PantryItem],
  val statusMessage:   StringProperty,
  val inventoryRepo:   DataRepository[PantryItem],
  val requestsRepo:    DataRepository[FamilyRequest],
  val styleSheetPath:  String,
  val stage:           JFXApp3.PrimaryStage
) {

  // Reload both repos into their buffers
  def reloadData(): Unit = {
    inventoryRepo.loadAll() match {
      case Success(itemsList) =>
        inventoryBuffer.clear()
        inventoryBuffer.addAll(itemsList)
      case Failure(errorEx) =>
        statusMessage.value = s"Failed to load inventory: ${errorEx.getMessage}"
    }

    requestsRepo.loadAll() match {
      case Success(reqsList) =>
        requestsBuffer.clear()
        requestsBuffer.addAll(reqsList)
      case Failure(errorEx) =>
        statusMessage.value = s"Failed to load requests: ${errorEx.getMessage}"
    }
  }

  // Helper for displaying delete confirmation dialogs
  def confirmDeletion(headerTextVal: String, contentTextVal: String): Boolean = {
    val alert = new Alert(Alert.AlertType.Confirmation) {
      initOwner(stage)
      title       = "Confirm Deletion"
      headerText  = headerTextVal
      contentText = contentTextVal
    }
    alert.dialogPane.value.stylesheets.add(styleSheetPath)
    val result = alert.showAndWait()
    result.contains(ButtonType.OK)
  }

  // ai-assisted: #8
  // why: Authored custom local handle method to shadow library implicits and satisfy S2-6 (onAction = handle) without triggering Scala 3 compiler warnings.
  def handle(handler: => Unit): javafx.event.EventHandler[javafx.event.ActionEvent] = {
    new javafx.event.EventHandler[javafx.event.ActionEvent] {
      override def handle(event: javafx.event.ActionEvent): Unit = handler
    }
  }
}
