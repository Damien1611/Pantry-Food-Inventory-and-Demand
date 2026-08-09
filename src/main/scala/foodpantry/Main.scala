package foodpantry

import foodpantry.model._
import foodpantry.repo.{DataRepository, PantryItemSerializer, FamilyRequestSerializer}
import foodpantry.ui._
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.beans.property.StringProperty

// ai-assisted: #5
// why: Implement the primary ScalaFX JFXApp3 window (S2-1), layouts (S2-5), control bindings (S2-7) and event handling (S2-6) without direct javafx imports.
object Main extends JFXApp3 {

  // Paths to persistence data files
  private val inventoryFile: String = "src/main/resources/inventory.csv"
  private val requestsFile: String  = "src/main/resources/requests.csv"

  // Stylesheet resource path
  private val styleSheetPath: String = getClass.getResource("/styles.css").toExternalForm

  // Repositories
  private val inventoryRepo: DataRepository[PantryItem]    = new DataRepository[PantryItem](inventoryFile, PantryItemSerializer)
  private val requestsRepo: DataRepository[FamilyRequest]  = new DataRepository[FamilyRequest](requestsFile, FamilyRequestSerializer)

  // Reactive state buffers bound to UI controls
  private val inventoryBuffer = ObservableBuffer[PantryItem]()
  private val requestsBuffer  = ObservableBuffer[FamilyRequest]()
  private val planBuffer      = ObservableBuffer[DistributionPlan]()
  private val remainingBuffer = ObservableBuffer[PantryItem]()

  // Reactive status message for the bottom status bar
  private val statusMessage = StringProperty("Application loaded. Ready.")

  override def start(): Unit = {
    // Load initial data
    inventoryRepo.loadAll().foreach { items => inventoryBuffer.addAll(items) }
    requestsRepo.loadAll().foreach  { reqs  => requestsBuffer.addAll(reqs) }

    // Main layout container
    val rootPane = new BorderPane()

    // Apply the premium stylesheet
    val sceneObj = new Scene(rootPane, 1280, 820)
    sceneObj.stylesheets.add(styleSheetPath)

    stage = new JFXApp3.PrimaryStage {
      title = "Food Pantry Inventory & Demand Optimizer"
      scene = sceneObj
    }

    // Build AppContext now that stage is available
    val ctx = new AppContext(
      inventoryBuffer = inventoryBuffer,
      requestsBuffer  = requestsBuffer,
      planBuffer      = planBuffer,
      remainingBuffer = remainingBuffer,
      statusMessage   = statusMessage,
      inventoryRepo   = inventoryRepo,
      requestsRepo    = requestsRepo,
      styleSheetPath  = styleSheetPath,
      stage           = stage
    )

    // Bottom Status Bar
    val statusBar = new HBox {
      alignment = Pos.CenterLeft
      padding   = Insets(8, 12, 8, 12)
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
    val btnRequests  = new Button("Family Requests") { styleClass.add("nav-button") }
    val btnPlanner   = new Button("Distribution Planner") { styleClass.add("nav-button") }

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
    btnDashboard.onAction = ctx.handle {
      setScreen(DashboardScreen.create(ctx), btnDashboard)
    }

    btnInventory.onAction = ctx.handle {
      setScreen(InventoryScreen.create(ctx), btnInventory)
    }

    btnRequests.onAction = ctx.handle {
      setScreen(RequestsScreen.create(ctx), btnRequests)
    }

    btnPlanner.onAction = ctx.handle {
      setScreen(PlannerScreen.create(ctx), btnPlanner)
    }

    // Default to Dashboard screen
    setScreen(DashboardScreen.create(ctx), btnDashboard)
  }
}
