package foodpantry

import java.time.LocalDate
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.beans.property.StringProperty
import scala.util.{Success, Failure}

// ai-assisted: #5
// why: Implement the primary ScalaFX JFXApp3 window (S2-1), layouts (S2-5), control bindings (S2-7) and event handling (S2-6) without direct javafx imports.
object PantryApp extends JFXApp3 {

  // Paths to persistence data files
  private val inventoryFile: String = "src/main/resources/inventory.csv"
  private val requestsFile: String = "src/main/resources/requests.csv"

  // Repositories
  private val inventoryRepo: DataRepository[PantryItem] = new DataRepository[PantryItem](inventoryFile, PantryItemSerializer)
  private val requestsRepo: DataRepository[FamilyRequest] = new DataRepository[FamilyRequest](requestsFile, FamilyRequestSerializer)

  // Reactive state buffers bound to UI controls
  private val inventoryBuffer = ObservableBuffer[PantryItem]()
  private val requestsBuffer = ObservableBuffer[FamilyRequest]()
  private val planBuffer = ObservableBuffer[DistributionPlan]()
  private val remainingBuffer = ObservableBuffer[PantryItem]()

  // List of primary food categories
  private val foodCategories = Seq(
    "Grains & Cereals",
    "Proteins",
    "Fruits & Vegetables",
    "Canned/Prepared Meals",
    "Condiments & Cooking Essentials",
    "Beverages",
    "Baby & Infant Items",
    "Non-Food Essentials"
  )

  // Reactive status message for the bottom status bar
  private val statusMessage = StringProperty("Application loaded. Ready.")

  // Helper method to refresh data from repositories
  private def reloadData(): Unit = {
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

  // Generate the next sequential item ID by scanning the current inventory.
  // Parses the numeric suffix from IDs like "ITEM-042" and returns the next one
  // formatted as a zero-padded 3-digit string (e.g. "ITEM-043").
  private def nextItemId(): String = {
    val existingNumbers = inventoryBuffer.toList.flatMap { item =>
      val parts = item.itemId.split("-")
      if (parts.length == 2) SerializationHelper.safeParseInt(parts(1)).toOption
      else None
    }
    val nextNumber = if (existingNumbers.isEmpty) 1 else existingNumbers.max + 1
    f"ITEM-$nextNumber%03d"
  }

  // ai-assisted: #8
  // why: Authored custom local handle method to shadow library implicits and satisfy S2-6 (onAction = handle) without triggering Scala 3 compiler warnings.
  private def handle(handler: => Unit): javafx.event.EventHandler[javafx.event.ActionEvent] = {
    new javafx.event.EventHandler[javafx.event.ActionEvent] {
      override def handle(event: javafx.event.ActionEvent): Unit = handler
    }
  }



  override def start(): Unit = {
    // Load initial data
    reloadData()

    // Main layout container
    val rootPane = new BorderPane()

    // Apply the premium stylesheet stylesheet
    val sceneObj = new Scene(rootPane, 1280, 820)
    sceneObj.stylesheets.add(getClass.getResource("/styles.css").toExternalForm)

    stage = new JFXApp3.PrimaryStage {
      title = "Food Pantry Inventory & Demand Optimizer"
      scene = sceneObj
    }

    // Bottom Status Bar
    val statusBar = new HBox {
      alignment = Pos.CenterLeft
      padding = Insets(8, 12, 8, 12)
      styleClass.add("status-bar")
      children = Seq(
        new Label {
          text <== statusMessage
          style = "-fx-text-fill: #94a3b8; -fx-font-weight: bold;"
        }
      )
    }
    rootPane.bottom = statusBar

    // Left Navigation Sidebar
    val navTitle = new Label {
      text = "FOOD PANTRY"
      styleClass.add("sidebar-title")
    }

    // Navigation buttons
    val btnDashboard = new Button("Dashboard") { styleClass.add("nav-button") }
    val btnInventory = new Button("Inventory") { styleClass.add("nav-button") }
    val btnRequests = new Button("Family Requests") { styleClass.add("nav-button") }
    val btnPlanner = new Button("Distribution Planner") { styleClass.add("nav-button") }

    val sidebar = new VBox {
      styleClass.add("sidebar")
      children = Seq(navTitle, btnDashboard, btnInventory, btnRequests, btnPlanner)
    }
    rootPane.left = sidebar

    // Navigation switching function
    def setScreen(screenPane: Pane, activeButton: Button): Unit = {
      rootPane.center = screenPane
      Seq(btnDashboard, btnInventory, btnRequests, btnPlanner).foreach { button =>
        button.styleClass.remove("nav-button-active")
      }
      activeButton.styleClass.add("nav-button-active")
    }

    // Event handlers for navigation switching
    btnDashboard.onAction = handle {
      setScreen(createDashboardScreen(), btnDashboard)
    }

    btnInventory.onAction = handle {
      setScreen(createInventoryScreen(), btnInventory)
    }

    btnRequests.onAction = handle {
      setScreen(createRequestsScreen(), btnRequests)
    }

    btnPlanner.onAction = handle {
      setScreen(createPlannerScreen(), btnPlanner)
    }

    // Default to Dashboard screen
    setScreen(createDashboardScreen(), btnDashboard)
  }

  // SCREEN 1: Dashboard Overview Screen
  private def createDashboardScreen(): Pane = {
    val titleLbl = new Label("Dashboard Overview") { styleClass.add("header-title") }
    val subtitleLbl = new Label("Real-time summary of inventory, urgent demands, and waste risks.") { styleClass.add("header-subtitle") }

    // Summary statistics computations (separated by unit types: kg vs units)
    val totalWeight = inventoryBuffer.filter(_.unitType == "kg").map(_.quantity).sum
    val totalUnits = inventoryBuffer.filter(_.unitType == "units").map(_.quantity).sum

    val todayDate = LocalDate.now()
    val expiringCount = inventoryBuffer.collect {
      case perishable: PerishableFood if perishable.expirationDate.isBefore(todayDate.plusDays(7)) && perishable.expirationDate.isAfter(todayDate.minusDays(1)) =>
        perishable.quantity
    }.sum

    val expiredCount = inventoryBuffer.collect {
      case perishable: PerishableFood if perishable.expirationDate.isBefore(todayDate) =>
        perishable.quantity
    }.sum

    val activeRequestsCount = requestsBuffer.size
    val highlyUrgentRequestsCount = requestsBuffer.count(req => req.urgencyLevel >= 4)

    // Layout cards for metrics
    val cardWeightStock = new VBox {
      styleClass.add("metric-card")
      children = Seq(
        new Label("Weight-Based Stock") { styleClass.add("metric-title") },
        new Label(f"$totalWeight%.2f kg") { styleClass.add("metric-value") }
      )
    }

    val cardUnitStock = new VBox {
      styleClass.add("metric-card")
      children = Seq(
        new Label("Count-Based Stock") { styleClass.add("metric-title") },
        new Label(f"$totalUnits%.0f units") { styleClass.add("metric-value") }
      )
    }

    val cardExpiringSoon = new VBox {
      styleClass.add("metric-card")
      children = Seq(
        new Label("Expiring Soon (<7 Days)") { styleClass.add("metric-title") },
        new Label(f"$expiringCount%.1f items") {
          styleClass.add("metric-value")
          if (expiringCount > 0.0) styleClass.add("metric-warning")
        }
      )
    }

    val cardExpired = new VBox {
      styleClass.add("metric-card")
      children = Seq(
        new Label("Expired (Waste Risk)") { styleClass.add("metric-title") },
        new Label(f"$expiredCount%.1f items") {
          styleClass.add("metric-value")
          if (expiredCount > 0.0) style = "-fx-text-fill: #ef4444;" // Red highlight
        }
      )
    }

    val cardRequests = new VBox {
      styleClass.add("metric-card")
      children = Seq(
        new Label("Pending Requests") { styleClass.add("metric-title") },
        new Label(s"$activeRequestsCount families") { styleClass.add("metric-value") }
      )
    }

    val cardUrgent = new VBox {
      styleClass.add("metric-card")
      children = Seq(
        new Label("Highly Urgent (Level 4+)") { styleClass.add("metric-title") },
        new Label(s"$highlyUrgentRequestsCount families") {
          styleClass.add("metric-value")
          if (highlyUrgentRequestsCount > 0) style = "-fx-text-fill: #ef4444;"
        }
      )
    }

    val flowPane = new FlowPane {
      hgap = 12
      vgap = 12
      padding = Insets(10, 0, 20, 0)
      children = Seq(cardWeightStock, cardUnitStock, cardExpiringSoon, cardExpired, cardRequests, cardUrgent)
    }

    // Table of high-risk items (expired or expiring soon)
    val riskItemsLabel = new Label("High-Risk Perishable Food Items") {
      style = "-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 10px 0 5px 0;"
    }

    val riskItems = inventoryBuffer.collect {
      case p: PerishableFood if p.expirationDate.isBefore(todayDate.plusDays(7)) => p
    }.toSeq.sortBy(_.expirationDate.toEpochDay)

    val riskTable = new TableView[PerishableFood](ObservableBuffer.from(riskItems)) {
      prefHeight = 220
      placeholder = new Label("No expired or expiring items. Excellent inventory health!") {
        style = "-fx-text-fill: #64748b;"
      }
    }

    val rColStatus = new TableColumn[PerishableFood, String]("Status") {
      cellValueFactory = { cd =>
        val p = cd.value
        val status = if (p.expirationDate.isBefore(todayDate)) "⚠ EXPIRED" else "⏳ EXPIRING SOON"
        StringProperty(status)
      }
      prefWidth = 130
    }
    val rColName = new TableColumn[PerishableFood, String]("Item Name") {
      cellValueFactory = { cd => StringProperty(cd.value.itemName) }
      prefWidth = 160
    }
    val rColCategory = new TableColumn[PerishableFood, String]("Category") {
      cellValueFactory = { cd => StringProperty(cd.value.category) }
      prefWidth = 220
    }
    val rColQty = new TableColumn[PerishableFood, String]("Quantity") {
      cellValueFactory = { cd => StringProperty(f"${cd.value.quantity}%.2f ${cd.value.unitType}") }
      prefWidth = 110
    }
    val rColExpiry = new TableColumn[PerishableFood, String]("Expiry Date") {
      cellValueFactory = { cd => StringProperty(cd.value.expirationDate.toString) }
      prefWidth = 110
    }
    val rColStorage = new TableColumn[PerishableFood, String]("Storage") {
      cellValueFactory = { cd => StringProperty(cd.value.storageTemp) }
      prefWidth = 110
    }

    riskTable.columns ++= Seq(rColStatus, rColName, rColCategory, rColQty, rColExpiry, rColStorage)

    val contentVBox = new VBox {
      padding = Insets(24)
      spacing = 10
      children = Seq(titleLbl, subtitleLbl, flowPane, riskItemsLabel, riskTable)
    }
    VBox.setVgrow(riskTable, javafx.scene.layout.Priority.ALWAYS)
    contentVBox
  }

  // SCREEN 2: Inventory Logging Screen
  private def createInventoryScreen(): Pane = {
    val titleLbl = new Label("Inventory Management") { styleClass.add("header-title") }
    val subtitleLbl = new Label("Log and manage food pantry items. Green color tags denote perishable food items.") { styleClass.add("header-subtitle") }

    // Table view to display items — grows to fill available vertical space
    val tableView = new TableView[PantryItem](inventoryBuffer)
    VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS)

    val colId = new TableColumn[PantryItem, String]("ID") {
      cellValueFactory = { cellData => StringProperty(cellData.value.itemId) }
      prefWidth = 80
    }
    val colName = new TableColumn[PantryItem, String]("Item Name") {
      cellValueFactory = { cellData => StringProperty(cellData.value.itemName) }
      prefWidth = 150
    }
    val colQty = new TableColumn[PantryItem, String]("Quantity") {
      cellValueFactory = { cellData => StringProperty(f"${cellData.value.quantity}%.2f") }
      prefWidth = 80
    }
    val colUnit = new TableColumn[PantryItem, String]("Unit") {
      cellValueFactory = { cellData => StringProperty(cellData.value.unitType) }
      prefWidth = 60
    }
    val colCategory = new TableColumn[PantryItem, String]("Category") {
      cellValueFactory = { cellData => StringProperty(cellData.value.category) }
      prefWidth = 210
    }
    val colDetails = new TableColumn[PantryItem, String]("Details / Expiration") {
      cellValueFactory = { cellData => StringProperty(cellData.value.displayDetails) }
      prefWidth = 480
    }

    tableView.columns ++= Seq(colId, colName, colQty, colUnit, colCategory, colDetails)

    // Form containers to log items
    val formTitle = new Label("Log New Donated Food Item") {
      style = "-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;"
    }

    val nameField = new TextField { promptText = "e.g., Organic Rice" }
    val qtyField = new TextField { promptText = "e.g., 25.5" }
    val categoryCombo = new ComboBox[String](foodCategories) { value = "Grains & Cereals" }
    val typeCombo = new ComboBox[String](Seq("Perishable", "ShelfStable")) { value = "Perishable" }
    val unitCombo = new ComboBox[String](Seq("kg", "units")) { value = "kg" }
    val dietaryCombo = new ComboBox[String](Seq("Standard", "Vegan", "Vegetarian", "Gluten-Free", "Halal")) { value = "Standard" }
    
    // Perishable specific fields
    val storageCombo = new ComboBox[String](Seq("Refrigerated", "Frozen")) { value = "Refrigerated" }
    val expiryPicker = new DatePicker { promptText = "Select Expiration Date" }

    // Toggle fields based on type combo change
    typeCombo.onAction = handle {
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

    // Input Validation (S1-18: Graceful input validation)
    def validateAndSubmit(): Unit = {
      val nameText = nameField.text.value.trim
      val qtyText = qtyField.text.value.trim
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
      inventoryRepo.add(item) match {
        case Success(_) =>
          statusMessage.value = s"Success: Logged ${item.itemName} (${item.quantity} ${item.unitType})"
          errorLabel.text = ""
          nameField.text = ""
          qtyField.text = ""
          expiryPicker.value = null
          reloadData()
        case Failure(errorEx) =>
          errorLabel.text = s"Persistence failed: ${errorEx.getMessage}"
      }
    }

    // ai-assisted: #9
    // why: Intercept Enter key presses on form TextFields using ScalaFX native KeyCode and KeyEvents to trigger submission (S2-10).
    val enterKeyHandler = (keyEvent: KeyEvent) => {
      if (keyEvent.code == KeyCode.Enter) {
        validateAndSubmit()
      }
    }
    nameField.onKeyPressed = enterKeyHandler
    qtyField.onKeyPressed = enterKeyHandler

    val btnSave = new Button("Add Inventory Item") {
      styleClass.add("btn-primary")
      onAction = handle { validateAndSubmit() }
    }

    // Table delete action button
    val btnDelete = new Button("Delete Selected Item") {
      styleClass.add("btn-danger")
      onAction = handle {
        val selectedItem = tableView.selectionModel.value.getSelectedItem
        if (selectedItem != null) {
          val alert = new Alert(Alert.AlertType.Confirmation) {
            initOwner(stage)
            title = "Confirm Deletion"
            headerText = "Delete Inventory Item"
            contentText = s"Are you sure you want to delete ${selectedItem.itemName}?"
          }
          alert.dialogPane.value.stylesheets.add(getClass.getResource("/styles.css").toExternalForm)
          val result = alert.showAndWait()
          if (result.contains(ButtonType.OK)) {
            inventoryRepo.delete(item => item.itemId == selectedItem.itemId) match {
              case Success(_) =>
                statusMessage.value = s"Deleted item: ${selectedItem.itemName}"
                reloadData()
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
      hgap = 10
      vgap = 10
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
      padding = Insets(24)
      spacing = 15
      children = Seq(titleLbl, subtitleLbl, tableView, formTitle, formGrid, errorLabel)
    }
    VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS)

    val scrollPane = new ScrollPane {
      content = contentBox
      fitToWidth = true
      fitToHeight = true
      style = "-fx-background-color: transparent; -fx-background: transparent;"
    }
    new StackPane { children = Seq(scrollPane) }
  }

  // SCREEN 3: Family Requests Logging Screen
  private def createRequestsScreen(): Pane = {
    val titleLbl = new Label("Family Request Management") { styleClass.add("header-title") }
    val subtitleLbl = new Label("Log and track requests from households in need.") { styleClass.add("header-subtitle") }

    // Table view to display family requests
    val tableView = new TableView[FamilyRequest](requestsBuffer) {
      prefHeight = 250
    }

    val colId = new TableColumn[FamilyRequest, String]("Request ID") {
      cellValueFactory = { cellData => StringProperty(cellData.value.requestId) }
      prefWidth = 100
    }
    val colName = new TableColumn[FamilyRequest, String]("Family Name") {
      cellValueFactory = { cellData => StringProperty(cellData.value.familyName) }
      prefWidth = 150
    }
    val colSize = new TableColumn[FamilyRequest, String]("Size") {
      cellValueFactory = { cellData => StringProperty(cellData.value.householdSize.toString) }
      prefWidth = 80
    }
    val colCategory = new TableColumn[FamilyRequest, String]("Category") {
      cellValueFactory = { cellData => StringProperty(cellData.value.requestCategory) }
      prefWidth = 210
    }
    val colDiet = new TableColumn[FamilyRequest, String]("Dietary Option") {
      cellValueFactory = { cellData => StringProperty(cellData.value.dietaryRestriction) }
      prefWidth = 120
    }
    val colUrgency = new TableColumn[FamilyRequest, String]("Urgency Level (1-5)") {
      cellValueFactory = { cellData => StringProperty(cellData.value.urgencyLevel.toString) }
      prefWidth = 160
    }

    tableView.columns ++= Seq(colId, colName, colSize, colCategory, colDiet, colUrgency)

    // Form
    val formTitle = new Label("Log New Family Request") {
      style = "-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 14px;"
    }

    val nameField = new TextField { promptText = "e.g., Henderson Family" }
    val sizeField = new TextField { promptText = "e.g., 4" }
    val categoryCombo = new ComboBox[String](foodCategories) { value = "Proteins" }
    val dietaryCombo = new ComboBox[String](Seq("Standard", "Vegan", "Vegetarian", "Gluten-Free", "Halal")) { value = "Standard" }
    val urgencyCombo = new ComboBox[String](Seq("1", "2", "3", "4", "5")) { value = "3" }

    val errorLabel = new Label {
      style = "-fx-text-fill: #ef4444; -fx-font-weight: bold;"
    }

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
            val urgencyVal = urgencyCombo.value.value.toInt
            val newRequestId = s"REQ-${System.currentTimeMillis() % 100000}"
            val newRequest = FamilyRequest(newRequestId, nameText, sizeVal, categoryCombo.value.value, dietaryCombo.value.value, urgencyVal)
            saveRequest(newRequest)
        }
      }
    }

    // Save Helper
    def saveRequest(req: FamilyRequest): Unit = {
      requestsRepo.add(req) match {
        case Success(_) =>
          statusMessage.value = s"Success: Logged request for ${req.familyName} (Size: ${req.householdSize})"
          errorLabel.text = ""
          nameField.text = ""
          sizeField.text = ""
          reloadData()
        case Failure(errorEx) =>
          errorLabel.text = s"Persistence failed: ${errorEx.getMessage}"
      }
    }

    // Keyboard submit listener (S2-10)
    val enterKeyHandler = (keyEvent: KeyEvent) => {
      if (keyEvent.code == KeyCode.Enter) {
        validateAndSubmit()
      }
    }
    nameField.onKeyPressed = enterKeyHandler
    sizeField.onKeyPressed = enterKeyHandler

    val btnSave = new Button("Submit Request") {
      styleClass.add("btn-primary")
      onAction = handle { validateAndSubmit() }
    }

    val btnDelete = new Button("Delete Selected Request") {
      styleClass.add("btn-danger")
      onAction = handle {
        val selectedReq = tableView.selectionModel.value.getSelectedItem
        if (selectedReq != null) {
          val alert = new Alert(Alert.AlertType.Confirmation) {
            initOwner(stage)
            title = "Confirm Deletion"
            headerText = "Delete Family Request"
            contentText = s"Are you sure you want to delete request for ${selectedReq.familyName}?"
          }
          alert.dialogPane.value.stylesheets.add(getClass.getResource("/styles.css").toExternalForm)
          val result = alert.showAndWait()
          if (result.contains(ButtonType.OK)) {
            requestsRepo.delete(req => req.requestId == selectedReq.requestId) match {
              case Success(_) =>
                statusMessage.value = s"Deleted request for ${selectedReq.familyName}"
                reloadData()
              case Failure(errorEx) =>
                errorLabel.text = s"Failed to delete request: ${errorEx.getMessage}"
            }
          }
        } else {
          errorLabel.text = "Select a request from the table to delete."
        }
      }
    }

    val formGrid = new GridPane {
      hgap = 10
      vgap = 10
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

    new VBox {
      padding = Insets(24)
      spacing = 15
      children = Seq(titleLbl, subtitleLbl, tableView, formTitle, formGrid, errorLabel)
    }
  }

  // SCREEN 4: Distribution Planner Screen
  private def createPlannerScreen(): Pane = {
    val titleLbl = new Label("Distribution Planner & Waste Reducer") { styleClass.add("header-title") }
    val subtitleLbl = new Label("Run the optimization engine to match pending family requests to expiring inventory items.") { styleClass.add("header-subtitle") }

    // Using object-level remainingBuffer to persist state across screen navigation

    // ── TABLE 1: Allocation Plan per Family ───────────────────────────────────
    val planTable = new TableView[DistributionPlan](planBuffer) {
      prefHeight = 230
      placeholder = new Label("Click 'Run Plan Optimization' to generate the allocation plan.") {
        style = "-fx-text-fill: #64748b;"
      }
    }

    val colFamily = new TableColumn[DistributionPlan, String]("Family Name") {
      cellValueFactory = { cd => StringProperty(cd.value.request.familyName) }
      prefWidth = 150
    }
    val colSize = new TableColumn[DistributionPlan, String]("Size") {
      cellValueFactory = { cd => StringProperty(cd.value.request.householdSize.toString) }
      prefWidth = 55
    }
    val colUrgency = new TableColumn[DistributionPlan, String]("Urgency") {
      cellValueFactory = { cd => StringProperty(cd.value.request.urgencyLevel.toString) }
      prefWidth = 65
    }
    val colCategory = new TableColumn[DistributionPlan, String]("Category") {
      cellValueFactory = { cd => StringProperty(cd.value.request.requestCategory) }
      prefWidth = 170
    }
    val colDiet = new TableColumn[DistributionPlan, String]("Dietary") {
      cellValueFactory = { cd => StringProperty(cd.value.request.dietaryRestriction) }
      prefWidth = 100
    }
    val colPlanDate = new TableColumn[DistributionPlan, String]("Plan Date") {
      cellValueFactory = { cd => StringProperty(cd.value.planDate.toString) }
      prefWidth = 100
    }
    val colAllocated = new TableColumn[DistributionPlan, String]("Allocated Items") {
      cellValueFactory = { cd =>
        val plan = cd.value
        val text =
          if (plan.allocations.isEmpty) {
            "*** NO MATCHING INVENTORY AVAILABLE ***"
          } else {
            plan.allocations.map { alloc =>
              f"${alloc.allocatedQuantity}%.2f ${alloc.item.unitType} of ${alloc.item.itemName}"
            }.mkString(" | ")
          }
        StringProperty(text)
      }
      prefWidth = 320
    }

    planTable.columns ++= Seq(colFamily, colSize, colUrgency, colCategory, colDiet, colPlanDate, colAllocated)

    // ── TABLE 2: Remaining Inventory After Allocation ─────────────────────────
    val remainingLabel = new Label("Projected Remaining Inventory After Allocation") {
      style = "-fx-text-fill: #f8fafc; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 0 4px 0;"
    }

    val remainingTable = new TableView[PantryItem](remainingBuffer) {
      prefHeight = 200
      placeholder = new Label("Run the planner to see remaining stock.") {
        style = "-fx-text-fill: #64748b;"
      }
    }

    val rColName = new TableColumn[PantryItem, String]("Item Name") {
      cellValueFactory = { cd => StringProperty(cd.value.itemName) }
      prefWidth = 170
    }
    val rColCategory = new TableColumn[PantryItem, String]("Category") {
      cellValueFactory = { cd => StringProperty(cd.value.category) }
      prefWidth = 175
    }
    val rColQty = new TableColumn[PantryItem, String]("Qty Remaining") {
      cellValueFactory = { cd => StringProperty(f"${cd.value.quantity}%.2f") }
      prefWidth = 120
    }
    val rColUnit = new TableColumn[PantryItem, String]("Unit") {
      cellValueFactory = { cd => StringProperty(cd.value.unitType) }
      prefWidth = 65
    }
    val rColStorage = new TableColumn[PantryItem, String]("Storage") {
      cellValueFactory = { cd => StringProperty(cd.value.storageTemp) }
      prefWidth = 110
    }
    val rColTag = new TableColumn[PantryItem, String]("Dietary Tag") {
      cellValueFactory = { cd => StringProperty(cd.value.dietaryTag) }
      prefWidth = 110
    }

    remainingTable.columns ++= Seq(rColName, rColCategory, rColQty, rColUnit, rColStorage, rColTag)

    // ── Buttons ───────────────────────────────────────────────────────────────
    val btnRun = new Button("Run Plan Optimization") {
      styleClass.add("btn-primary")
    }

    val btnSavePlan = new Button("Export Distribution Plan") {
      styleClass.add("btn-primary")
      disable = planBuffer.isEmpty
    }

    btnRun.onAction = handle {
      val inventoryItems = inventoryBuffer.toList
      val familyRequests = requestsBuffer.toList

      if (inventoryItems.isEmpty) {
        statusMessage.value = "Cannot run planner: Inventory is completely empty."
      } else if (familyRequests.isEmpty) {
        statusMessage.value = "Cannot run planner: No pending family requests found."
      } else {
        val (generatedPlans, remaining) = MatchingEngine.generatePlan(inventoryItems, familyRequests)

        // Populate plan table
        planBuffer.clear()
        planBuffer.addAll(generatedPlans)

        // Populate remaining inventory table (only items with stock left)
        remainingBuffer.clear()
        remainingBuffer.addAll(remaining.filter(_.quantity > 0.0))

        btnSavePlan.disable = false
        statusMessage.value = s"Plan generated: ${generatedPlans.size} families processed, ${remaining.count(_.quantity > 0.0)} items remaining in stock."
      }
    }

    // ai-assisted: #10
    // why: Export distribution plan and remaining stock report to file using PrintWriter wrapped in a safe scala.util.Try block with finally resource cleanup.
    btnSavePlan.onAction = handle {
      val exportPath = "src/main/resources/distribution_plan_report.txt"
      scala.util.Try {
        val pw = new java.io.PrintWriter(new java.io.File(exportPath), "UTF-8")
        try {
          pw.println(s"DAILY DISTRIBUTION PLAN — ${LocalDate.now()}")
          pw.println("=" * 72)
          planBuffer.foreach { plan =>
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
          remainingBuffer.foreach { item =>
            pw.println(f"  ${item.itemName.padTo(25, ' ')} | ${item.category.padTo(22, ' ')} | ${item.quantity}%.2f ${item.unitType}")
          }
        } finally {
          pw.close()
        }
      } match {
        case Success(_) =>
          statusMessage.value = "Success: Distribution plan saved to src/main/resources/distribution_plan_report.txt"
        case Failure(errorEx) =>
          statusMessage.value = s"Failed to export report: ${errorEx.getMessage}"
      }
    }

    val actionRow = new HBox {
      spacing = 15
      children = Seq(btnRun, btnSavePlan)
    }

    new VBox {
      padding = Insets(24)
      spacing = 12
      children = Seq(titleLbl, subtitleLbl, actionRow, planTable, remainingLabel, remainingTable)
    }
  }
}
