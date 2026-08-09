# Personal Reflection

This reflection evaluates the design, implementation, and object-oriented programming (OOP) principles applied in the development of the Food Pantry Inventory & Demand application.

## Sub-point 1: OOP Concepts Applied
In this project, several advanced object-oriented programming concepts were applied to model the food pantry's resource allocation problem:
- **Inheritance and Sealed Traits**: A sealed trait `PantryItem` was established as the base for all inventory items, subclassed by `PerishableFood` (perishable) and `ShelfStableFood` (shelf-stable). The `sealed` modifier ensures compile-time safety, preventing arbitrary external subclasses and enabling exhaustive pattern matching inside serialization and matching engine algorithms.
- **Subtype Polymorphism**: The `PantryItem` trait declares an abstract method `displayDetails`, which is explicitly overridden in `PerishableFood` (to display expiration dates, storage, and dietary categories) and `ShelfStableFood` (to display storage, unit types, and dietary tags).
- **Parametric Polymorphism**: A generic repository class `DataRepository[T]` and generic serializer interface `RecordSerializer[T]` were designed to handle data storage. This allows the same database reading and writing logic to serve both `PantryItem` and `FamilyRequest` records, keeping the codebase DRY.
- **Encapsulation**: Private fields, such as `private val filePath` in the repository, encapsulate internal file paths and state variables, preventing client classes from tampering with underlying persistence logic.
- **Immutability**: All model classes (`PerishableFood`, `ShelfStableFood`, `FamilyRequest`, `DistributionPlan`) are represented by immutable case classes. State transitions are achieved by returning new copies of items with modified quantities using recursive functions instead of mutable loops or collections.

## Sub-point 2: Key Problems and Solutions
During implementation, several design problems were encountered and solved:
- **ScalaFX TableView Reassignment**: Initially, reassigning lists directly to `tableView.columns` resulted in a reassignment compilation error because columns are defined as read-only values. The solution was using the `++=` operator to add columns in-place, which conforms to the ScalaFX API.
- **Unused Warning with Event Handlers**: Compiling with the `-Wunused:all` compiler flag flagged the ScalaFX `handle` implicit handler wrappers as warnings in Scala 3 due to old-style implicit parameter definitions inside the external dependency jar. To achieve zero warnings, a dual solution was required: a custom `handle` method was authored locally in `AppContext.scala` (originally in `PantryApp.scala` before modularization) to shadow the default implicit-based event handler conversion on the closure block, and the `"-Wconf:msg=Implicit parameters:s"` compiler option was configured in `build.sbt` to silence warnings triggered when assigning events to ScalaFX property setters like `onAction`.

## Sub-point 3: Strengths and Weaknesses of the Design
The current implementation has distinct characteristics:
- **Strengths**: The repository and logic layers are completely decoupled from the ScalaFX graphical user interface. The waste-minimizing allocation engine is a pure mathematical function, making it highly testable and robust. Exception handling via `scala.util.Try` ensures that invalid file formats or parsing failures do not lead to application crashes.
- **Weaknesses**: Although the application leverages reactive properties (`ObservableBuffer`), updating a record requires reloading the entire dataset from the CSV file. Introducing an in-memory cached state service would improve UI performance for larger datasets.
