# Developer Log

This document records the daily progress and design decisions for the Food Pantry Inventory & Demand application.

## 2026-07-24
- **Activity**: Project planning and requirements analysis.
- **Details**: Reviewed UN SDG 1 (No Poverty) and identified food pantry distribution matching as a key sub-domain. Sketched domain models showing sealed traits and subclasses for inventory management (S1-7).
- **Decisions**: Chose to persistent data using safe CSV serialization to avoid external database overhead.

## 2026-07-25
- **Activity**: Core domain model and repository implementation.
- **Details**: Implemented the `PantryItem` sealed trait hierarchy and concrete case classes: `PerishableFood` (perishable) and `ShelfStableFood` (shelf-stable) inside `DomainModel.scala`. Created the generic serialization trait `RecordSerializer[T]` and implemented the serializers in `Serialization.scala`.
- **Decisions**: Explicitly used `scala.util.Try` for all parsing and number conversions to avoid unhandled runtime crashes (S1-12).

## 2026-07-26
- **Activity**: Persistence layer and logic implementation.
- **Details**: Authored the generic `DataRepository[T]` class in `Repository.scala` to handle CRUD file-level operations. Programmed the `MatchingEngine` class in `MatchingEngine.scala`.
- **Decisions**: Decided on a tail-recursive greedy matching algorithm to avoid mutable loop iterations and guarantee domain-layer immutability (S1-11).

## 2026-07-27
- **Activity**: ScalaFX UI design and styling.
- **Details**: Wired up the `JFXApp3` structure in `PantryApp.scala`. Designed a side-bar navigation menu and modular panes (Dashboard, Inventory, Requests, Planner).
- **Decisions**: Added a custom `handle` method in `PantryApp` to override implicit warnings in Scala 3 compiler under `-Wunused`. Wrote `styles.css` using modern slate-dark aesthetics with distinct padding and borders.

## 2026-07-28
- **Activity**: Polish, validation, and documentation.
- **Details**: Implemented keyboard form submission (pressing Enter) and rigorous input verification. Generated class diagrams and finalized reflections.
- **Decisions**: Tested build clean compile cycle with `-Wunused` flags to ensure zero warning compiler status.

## 2026-08-05
- **Activity**: Rubric compliance modifications and styling improvements.
- **Details**: Adjusted `MatchingEngine.scala` to exclude expired PerishableFood items and enforce exact dietary restriction matches for non-Standard requests. Refactored `PantryApp.scala` to move `remainingBuffer` to object scope, preserving planner screen state during navigation. Introduced ScalaFX confirmation dialogs with custom CSS styling for dark theme readability.
- **Decisions**: Linked the application stylesheet directly to Alert popups to resolve text contrast issues.

## 2026-08-08
- **Activity**: Sample data expansion and UI layout improvements.
- **Details**: Populated `inventory.csv` with 38 diverse items (18 perishable, 20 shelf-stable) and `requests.csv` with 20 family requests across all categories and dietary tags. Replaced the dashboard's `ListView[String]` risk display with a structured `TableView[PerishableFood]` showing status, name, category, quantity, expiry date, and storage columns. Made the inventory screen table dynamically fill available vertical space using `VBox.setVgrow(ALWAYS)` and wrapped the screen in a `ScrollPane` for windowed mode usability. Increased the default window size from 1000×680 to 1280×820.
- **Decisions**: Added high-contrast scrollbar CSS rules (`.scroll-bar .thumb` in slate-600, hover in sky-400) so users can clearly identify and drag the scrollbar thumb against the dark track background across all tables.
