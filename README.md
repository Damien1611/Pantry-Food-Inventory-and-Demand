# Food Pantry Inventory & Demand Optimizer (SDG 1 — No Poverty)

A standalone ScalaFX desktop application designed to support food pantry coordinators in optimizing resource distribution, tracking expiration-based waste risk, and matching available inventory to households in need.

This application directly addresses **UN Sustainable Development Goal 1: No Poverty** by assisting non-governmental organizations (NGOs) and community groups in maximizing the utility of donated items and ensuring food resources are safely and equitably distributed to low-income households.

---

## Developer and Submission Details
- **Student Name**: Damien Yee Jin Yang
- **Student ID**: 23080633
- **Programme**: BSC (HONS) COMPUTER SCIENCE
- **Cohort**: 2026/02
- **Project Title**: Food Pantry Inventory & Demand Optimizer

---

## Key Features

1. **Dashboard Analytics**: Displays real-time metrics on total stock, perishable vs. non-perishable distribution, urgent pending requests, and aggregates warnings for expired or expiring items.
2. **Inventory Management**: Features an interactive table view where administrators can add new donations (validating date selections and quantities) or delete items. perishable items are visually formatted.
3. **Family Request Management**: Logs household requests, household sizes, dietary constraints (Standard, Vegan, Gluten-Free, Halal), and urgency levels.
4. **Distribution Planner**: Runs a waste-minimization matching algorithm that pairs family demands with inventory, prioritizing items closest to expiration and matching dietary restrictions. It generates a detailed report and exports it to `src/main/resources/distribution_plan_report.txt`.

---

## Development Platform & Setup

- **Java Development Kit (JDK)**: Java 21+ (Tested on Java 26)
- **Build Tool**: sbt 1.9+ (Configured with sbt 2.0.4)
- **Language**: Scala 3.3+ (Configured with Scala 3.8.4)
- **GUI Library**: ScalaFX 21 (uses JavaFX 21 underlying platform)

### Setup and Run Instructions

1. **Extract and Navigate**:
   Open a terminal and navigate to the project directory:
   ```bash
   cd Project_23080633
   ```

2. **Clean & Compile**:
   Compile the source code using the following sbt command (configured with strict compile-time warning flags):
   ```bash
   sbt clean compile
   ```
   *Note: This command will run with `-Wunused:all` and result in zero warning lines.*

3. **Launch the Application**:
   Run the application:
   ```bash
   sbt run
   ```
   A graphical window titled **"Food Pantry Inventory & Demand Optimizer"** will launch within seconds.

4. **Run Unit Tests (Optional)**:
   ```bash
   sbt test
   ```

---

## Project Structure
```
Project_23080633/
├── README.md                  # Setup, run, project summary, AI summary
├── build.sbt                  # Scala 3.8.4 + ScalaFX 21 configuration
├── project/
│   └── build.properties       # sbt version (2.0.4)
├── src/
│   ├── main/
│   │   ├── scala/             # Production source code
│   │   │   ├── DomainModel.scala
│   │   │   ├── Serialization.scala
│   │   │   ├── Repository.scala
│   │   │   ├── MatchingEngine.scala
│   │   │   └── PantryApp.scala
│   │   └── resources/         # Sample database and styles
│   │       ├── inventory.csv
│   │       ├── requests.csv
│   │       └── styles.css
├── docs/
│   ├── UML.png                # Architecture Class Diagram
│   ├── reflection.md          # Personal Reflection on OOP (350-700 words)
│   ├── ai_reflection.md       # AI Integration Reflection (300-500 words)
│   ├── dev_log.md             # Developer Log (5 dated entries)
│   ├── citations.md           # Citations & Licenses (ScalaFX, JavaFX)
│   └── demo.mp4               # Application walkthrough video placeholder
└── ai/
    ├── interaction_log.md     # AI Prompt & Refinement Log (10 entries)
    └── declaration.md         # AI Use Declaration (Signed)
```

---

## AI-Use & Citations Summary

- **AI Tools Used**: Gemini. Used for initial model scaffolding, CSS stylesheet design, and resolving ScalaFX event handling conventions.
- **Citations**: Detailed library links and license texts (BSD 3-Clause, GPL) are located in [citations.md](file:///c:/PRG2104/Project_23080633/docs/citations.md).
- **Walkthrough Video**: The `docs/demo.mp4` file is currently a placeholder. Please record a brief screen walkthrough of the running application on your device demonstrating the four features, name it `demo.mp4`, and place it in the `docs/` folder to complete the submission files before packaging the final ZIP.
