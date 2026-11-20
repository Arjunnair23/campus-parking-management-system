# Campus Parking Management System

A Java console application demonstrating core **Data Structures** using a campus parking scenario:

| Data Structure | Implementation | Used For |
|---|---|---|
| Singly Linked List | `ParkedVehicleList.java` (via `Node.java`) | Currently parked vehicles — insert, delete, traverse, search |
| Stack (linked-list based) | `LinkedStack.java` | Exit history (LIFO) + bracket-balance checker |
| Queue (linked-list based) | `LinkedQueue.java` | Waiting vehicles when lot is full (FIFO) |
| Stack application | `ExpressionHandler.java` | Bracket-balance validation of gate/zone codes, e.g. `{[A-1](B-2)}` |

`ParkingSystem.java` coordinates list + stack + queue into one workflow, `Main.java` is the CLI menu entry point.

## Project Structure

```
CampusParkingManagementSystem/
├── src/
│   ├── Vehicle.java
│   ├── Node.java
│   ├── ParkedVehicleList.java
│   ├── LinkedStack.java
│   ├── LinkedQueue.java
│   ├── ExpressionHandler.java
│   ├── ParkingSystem.java
│   └── Main.java
├── bin/   (compiled .class files go here after javac)
└── README.md
```

## How to Compile / Run

Requires JDK 8+ (tested on JDK 21).

**Windows (PowerShell / CMD):**
```bat
cd CampusParkingManagementSystem
javac -d bin src\*.java
java -cp bin Main
```

**Linux / macOS:**
```bash
cd CampusParkingManagementSystem
javac -d bin src/*.java
java -cp bin Main
```

## Menu

```
1. Park Vehicle (List insert / Queue if full)
2. Exit Vehicle (List delete / Stack push)
3. Search Vehicle (List search)
4. Display Parked Vehicles (List traverse)
5. Display Waiting Queue (Queue traverse)
6. Display Exit History (Stack traverse)
7. Validate Gate Code (Stack bracket-checker)
8. Exit
```

## Workflow Logic

- **Park:** if `parked.size < capacity` (default 5) → insert into linked list, else → `enqueue` to waiting queue.
- **Exit:** `delete` from linked list by plate → `push` onto exit-history stack → if queue non-empty, `dequeue` first waiting vehicle and auto-park it.
- **Gate code:** `ExpressionHandler.isBalanced()` ignores non-bracket chars and checks `()`, `{}`, `[]` nesting with `LinkedStack<Character>`.
