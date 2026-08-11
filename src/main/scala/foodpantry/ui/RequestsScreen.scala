package foodpantry.ui

import foodpantry.model._
import foodpantry.repo.{SerializationHelper}
import scala.util.{Success, Failure}
import scalafx.geometry.Insets
import scalafx.scene.control._
import scalafx.scene.layout._

// ── SCREEN 3: Family Requests Logging Screen ──────────────────────────────────
// Allows coordinators to log new household demand requests and delete them.
object RequestsScreen extends UIHelpers {

  def create(ctx: AppContext): Pane = {
    // ── Screen Headers ───────────────────────────────────────────────────────
    val titleLbl    = new Label("Family Request Management") { styleClass.add("header-title") }
    val subtitleLbl = new Label("Log and track requests from households in need.") { styleClass.add("header-subtitle") }

    // ── Family Requests Table ────────────────────────────────────────────────
    // Table view to display family requests
    val tableView = new TableView[FamilyRequest](ctx.requestsBuffer) {
      prefHeight = 250
    }

    val colId       = strColumn[FamilyRequest]("Request ID", 100)(_.requestId)
    val colName     = strColumn[FamilyRequest]("Family Name", 150)(_.familyName)
    val colSize     = strColumn[FamilyRequest]("Size", 80)(_.householdSize.toString)
    val colCategory = strColumn[FamilyRequest]("Category", 210)(_.requestCategory)
    val colDiet     = strColumn[FamilyRequest]("Dietary Option", 120)(_.dietaryRestriction)
    val colUrgency  = strColumn[FamilyRequest]("Urgency Level (1-5)", 160)(_.urgencyLevel.toString)

    tableView.columns ++= Seq(colId, colName, colSize, colCategory, colDiet, colUrgency)

    // ── Form Input Fields ────────────────────────────────────────────────────
    val formTitle = new Label("Log New Family Request") {
      style = "-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;"
    }

    val nameField     = new TextField { promptText = "e.g., Henderson Family" }
    val sizeField     = new TextField { promptText = "e.g., 4" }
    val categoryCombo = new ComboBox[String](foodCategories) { value = "Proteins" }
    val dietaryCombo  = new ComboBox[String](Seq("Standard", "Vegan", "Vegetarian", "Gluten-Free", "Halal")) { value = "Standard" }
    val urgencyCombo  = new ComboBox[String](Seq("1", "2", "3", "4", "5")) { value = "3" }

    val errorLabel = new Label {
      style = "-fx-text-fill: #ef4444; -fx-font-weight: bold;"
    }

    // ── Form Input Validation & Submission ────────────────────────────────────
    // Input Validation (S1-18: Graceful input validation)
    def validateAndSubmit(): Unit = {
      val nameText = nameField.text.value.trim
      val sizeText = sizeField.text.value.trim

      if (nameText.isEmpty) {
        errorLabel.text = "Error: Family Name cannot be empty."
      } else if (sizeText.isEmpty) {
        errorLabel.text = "Error: Household size cannot be empty."
      } else {
        SerializationHelper.safeParseInt(sizeText) match {
          case Failure(_) =>
            errorLabel.text = "Error: Household size must be a valid integer."
          case Success(sizeVal) if sizeVal <= 0 =>
            errorLabel.text = "Error: Household size must be greater than zero."
          case Success(sizeVal) =>
            val urgencyVal    = urgencyCombo.value.value.toInt
            val newRequestId  = s"REQ-${System.currentTimeMillis() % 100000}"
            val newRequest    = FamilyRequest(newRequestId, nameText, sizeVal, categoryCombo.value.value, dietaryCombo.value.value, urgencyVal)
            saveRequest(newRequest)
        }
      }
    }

    // ── Save Function ────────────────────────────────────────────────────────
    def saveRequest(req: FamilyRequest): Unit = {
      ctx.requestsRepo.add(req) match {
        case Success(_) =>
          ctx.statusMessage.value = s"Success: Logged request for ${req.familyName} (Size: ${req.householdSize})"
          errorLabel.text = ""
          nameField.text  = ""
          sizeField.text  = ""
          ctx.reloadData()
        case Failure(errorEx) =>
          errorLabel.text = s"Persistence failed: ${errorEx.getMessage}"
      }
    }

    // Bind Enter keyboard shortcut for text fields
    submitOnEnter(nameField, sizeField)(validateAndSubmit())

    // ── Primary Action Buttons ───────────────────────────────────────────────
    val btnSave = new Button("Submit Request") {
      styleClass.add("btn-primary")
      onAction = ctx.handle { validateAndSubmit() }
    }

    // Delete selected request button with popup confirmation dialog
    val btnDelete = new Button("Delete Selected Request") {
      styleClass.add("btn-danger")
      onAction = ctx.handle {
        val selectedReq = tableView.selectionModel.value.getSelectedItem
        if (selectedReq != null) {
          if (ctx.confirmDeletion("Delete Family Request", s"Are you sure you want to delete request for ${selectedReq.familyName}?")) {
            ctx.requestsRepo.delete(req => req.requestId == selectedReq.requestId) match {
              case Success(_) =>
                ctx.statusMessage.value = s"Deleted request for ${selectedReq.familyName}"
                ctx.reloadData()
              case Failure(errorEx) =>
                errorLabel.text = s"Failed to delete request: ${errorEx.getMessage}"
            }
          }
        } else {
          errorLabel.text = "Select a request from the table to delete."
        }
      }
    }

    // ── Layout Grid Configuration ─────────────────────────────────────────────
    val formGrid = new GridPane {
      hgap    = 10
      vgap    = 10
      padding = Insets(10)
      styleClass.add("form-container")

      add(new Label("Family Name:") { styleClass.add("form-label") }, 0, 0)
      add(nameField, 1, 0)

      add(new Label("Household Size:") { styleClass.add("form-label") }, 0, 1)
      add(sizeField, 1, 1)

      add(new Label("Requested Category:") { styleClass.add("form-label") }, 0, 2)
      add(categoryCombo, 1, 2)

      add(new Label("Dietary Restr:") { styleClass.add("form-label") }, 0, 3)
      add(dietaryCombo, 1, 3)

      add(new Label("Urgency (1-5):") { styleClass.add("form-label") }, 0, 4)
      add(urgencyCombo, 1, 4)

      add(btnSave, 0, 5)
      add(btnDelete, 1, 5)
    }

    // ── Screen VBox Assembly ─────────────────────────────────────────────────
    new VBox {
      padding  = Insets(24)
      spacing  = 15
      children = Seq(titleLbl, subtitleLbl, tableView, formTitle, formGrid, errorLabel)
    }
  }
}
