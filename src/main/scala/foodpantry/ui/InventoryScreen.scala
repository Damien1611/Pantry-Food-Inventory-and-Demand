package foodpantry.ui

import foodpantry.model._
import foodpantry.repo.SerializationHelper
import java.time.LocalDate
import scala.util.{Success, Failure}
import scalafx.geometry.Insets
import scalafx.collections.ObservableBuffer
import scalafx.scene.control._
import scalafx.scene.layout._

// SCREEN 2: Inventory Logging Screen
object InventoryScreen extends UIHelpers {

  def create(ctx: AppContext): Pane = {
    val titleLbl    = new Label("Inventory Management") { styleClass.add("header-title") }
    val subtitleLbl = new Label("Log and manage food pantry items. Green color tags denote perishable food items.") { styleClass.add("header-subtitle") }

    // Table view to display items — grows to fill available vertical space
    val tableView = new TableView[PantryItem](ctx.inventoryBuffer)
    VBox.setVgrow(tableView, Priority.Always)

    val colId       = strColumn[PantryItem]("ID", 80)(_.itemId)
    val colName     = strColumn[PantryItem]("Item Name", 150)(_.itemName)
    val colQty      = strColumn[PantryItem]("Quantity", 80)(item => f"${item.quantity}%.2f")
    val colUnit     = strColumn[PantryItem]("Unit", 60)(_.unitType)
    val colCategory = strColumn[PantryItem]("Category", 210)(_.category)
    val colDetails  = strColumn[PantryItem]("Details / Expiration", 480)(_.displayDetails)

    tableView.columns ++= Seq(colId, colName, colQty, colUnit, colCategory, colDetails)

    // Form containers to log items
    val formTitle = new Label("Log New Donated Food Item") {
      style = "-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;"
    }

    val nameField     = new TextField { promptText = "e.g., Organic Rice" }
    val qtyField      = new TextField { promptText = "e.g., 25.5" }
    val categoryCombo = new ComboBox[String](foodCategories) { value = "Grains & Cereals" }
    val typeCombo     = new ComboBox[String](Seq("Perishable", "ShelfStable")) { value = "Perishable" }
    val unitCombo     = new ComboBox[String](Seq("kg", "units")) { value = "kg" }
    val dietaryCombo  = new ComboBox[String](Seq("Standard", "Vegan", "Vegetarian", "Gluten-Free", "Halal")) { value = "Standard" }

    // Perishable specific fields
    val storageCombo = new ComboBox[String](Seq("Refrigerated", "Frozen")) { value = "Refrigerated" }
    val expiryPicker = new DatePicker { promptText = "Select Expiration Date" }

    // Toggle fields based on type combo change
    typeCombo.onAction = ctx.handle {
      val isPerishable = typeCombo.value.value == "Perishable"
      expiryPicker.visible = isPerishable
      expiryPicker.managed = isPerishable
      if (isPerishable) {
        storageCombo.items = ObservableBuffer.from(Seq("Refrigerated", "Frozen"))
        storageCombo.value = "Refrigerated"
      } else {
        storageCombo.items = ObservableBuffer.from(Seq("Dry storage"))
        storageCombo.value = "Dry storage"
      }
    }

    val errorLabel = new Label {
      style = "-fx-text-fill: #ef4444; -fx-font-weight: bold;"
    }

    // Generate the next sequential item ID by scanning the current inventory.
    def nextItemId(): String = {
      val existingNumbers = ctx.inventoryBuffer.toList.flatMap { item =>
        val parts = item.itemId.split("-")
        if (parts.length == 2) SerializationHelper.safeParseInt(parts(1)).toOption
        else None
      }
      val nextNumber = if (existingNumbers.isEmpty) 1 else existingNumbers.max + 1
      f"ITEM-$nextNumber%03d"
    }

    // Input Validation (S1-18: Graceful input validation)
    def validateAndSubmit(): Unit = {
      val nameText     = nameField.text.value.trim
      val qtyText      = qtyField.text.value.trim
      val isPerishable = typeCombo.value.value == "Perishable"

      if (nameText.isEmpty) {
        errorLabel.text = "Error: Item Name cannot be empty."
      } else if (qtyText.isEmpty) {
        errorLabel.text = "Error: Quantity cannot be empty."
      } else {
        // Safe double parsing
        SerializationHelper.safeParseDouble(qtyText) match {
          case Failure(_) =>
            errorLabel.text = "Error: Quantity must be a valid number (e.g. 10.5)."
          case Success(quantityVal) if quantityVal <= 0.0 =>
            errorLabel.text = "Error: Quantity must be greater than zero."
          case Success(quantityVal) =>
            if (isPerishable) {
              val dateVal = expiryPicker.value.value
              if (dateVal == null) {
                errorLabel.text = "Error: Expiration date must be selected for perishable items."
              } else if (dateVal.isBefore(LocalDate.now())) {
                errorLabel.text = "Error: Expiration date cannot be in the past."
              } else {
                val newItemId = nextItemId()
                val newItem = PerishableFood(
                  newItemId,
                  nameText,
                  quantityVal,
                  categoryCombo.value.value,
                  dateVal,
                  storageCombo.value.value,
                  dietaryCombo.value.value,
                  unitCombo.value.value
                )
                saveItem(newItem)
              }
            } else {
              val newItemId = nextItemId()
              val newItem = ShelfStableFood(
                newItemId,
                nameText,
                quantityVal,
                categoryCombo.value.value,
                "Dry storage",
                dietaryCombo.value.value,
                unitCombo.value.value
              )
              saveItem(newItem)
            }
        }
      }
    }

    // Save helper
    def saveItem(item: PantryItem): Unit = {
      ctx.inventoryRepo.add(item) match {
        case Success(_) =>
          ctx.statusMessage.value = s"Success: Logged ${item.itemName} (${item.quantity} ${item.unitType})"
          errorLabel.text = ""
          nameField.text  = ""
          qtyField.text   = ""
          expiryPicker.value = null
          ctx.reloadData()
        case Failure(errorEx) =>
          errorLabel.text = s"Persistence failed: ${errorEx.getMessage}"
      }
    }

    submitOnEnter(nameField, qtyField)(validateAndSubmit())

    val btnSave = new Button("Add Inventory Item") {
      styleClass.add("btn-primary")
      onAction = ctx.handle { validateAndSubmit() }
    }

    // Table delete action button
    val btnDelete = new Button("Delete Selected Item") {
      styleClass.add("btn-danger")
      onAction = ctx.handle {
        val selectedItem = tableView.selectionModel.value.getSelectedItem
        if (selectedItem != null) {
          if (ctx.confirmDeletion("Delete Inventory Item", s"Are you sure you want to delete ${selectedItem.itemName}?")) {
            ctx.inventoryRepo.delete(item => item.itemId == selectedItem.itemId) match {
              case Success(_) =>
                ctx.statusMessage.value = s"Deleted item: ${selectedItem.itemName}"
                ctx.reloadData()
              case Failure(errorEx) =>
                errorLabel.text = s"Failed to delete item: ${errorEx.getMessage}"
            }
          }
        } else {
          errorLabel.text = "Select an item from the table to delete."
        }
      }
    }

    val formGrid = new GridPane {
      hgap    = 10
      vgap    = 10
      padding = Insets(10)
      styleClass.add("form-container")

      add(new Label("Item Name:") { styleClass.add("form-label") }, 0, 0)
      add(nameField, 1, 0)

      add(new Label("Quantity:") { styleClass.add("form-label") }, 0, 1)
      add(qtyField, 1, 1)
      add(unitCombo, 2, 1)

      add(new Label("Category:") { styleClass.add("form-label") }, 0, 2)
      add(categoryCombo, 1, 2)

      add(new Label("Food Type:") { styleClass.add("form-label") }, 0, 3)
      add(typeCombo, 1, 3)

      add(new Label("Storage Requirement:") { styleClass.add("form-label") }, 0, 4)
      add(storageCombo, 1, 4)

      add(new Label("Details:") { styleClass.add("form-label") }, 0, 5)
      add(expiryPicker, 1, 5)

      add(new Label("Dietary Tag:") { styleClass.add("form-label") }, 0, 6)
      add(dietaryCombo, 1, 6)

      add(btnSave, 0, 7)
      add(btnDelete, 1, 7)
    }

    val contentBox = new VBox {
      padding  = Insets(24)
      spacing  = 15
      children = Seq(titleLbl, subtitleLbl, tableView, formTitle, formGrid, errorLabel)
    }
    VBox.setVgrow(tableView, Priority.Always)

    val scrollPane = new ScrollPane {
      content      = contentBox
      fitToWidth   = true
      fitToHeight  = true
      style        = "-fx-background-color: transparent; -fx-background: transparent;"
    }
    new StackPane { children = Seq(scrollPane) }
  }
}
