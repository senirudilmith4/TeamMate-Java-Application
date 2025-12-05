
# 🧩TeamMate Application

A smart automated team-formation system built with a focus on balanced grouping, clean architecture, and scalable design.

📘 Overview

TeamMate Application is designed to help organizers create fair, well-balanced teams by analyzing participant data such as roles, skill levels, and personality types.
The system emphasizes modular design, object-oriented principles, concurrency, and data consistency, making it both efficient and extensible.


## ✨ Features

- Light/dark mode toggle
- Live previews
- Fullscreen mode
- Cross platform


## 🛠 Tech Stack
#### Languages & Tools

- Java 17+

- JUnit 5

- Maven

- Java Collections & Custom Data Structures

- ExecutorService Concurrency

- CSV handling via Java I/O

#### Design Focus

- Object-Oriented Architecture

- Clear separation of concerns

- Extensible service-based structure

- UML-driven design (Use Case, Class, Sequence, Activity diagrams)


## 🏗 Architecture



├── model/           → Participant, Team, Role, PersonalityType

├── service/         → TeamBuilder, CSVHandler, SurveyProcess

├── exception/       → Custom exception classes

├── login/          → Organizer login & Organizer

└── app/             → Main entry point & controller
## 🚀 Installation & Running

Clone the Repository

```bash
  git clone https://github.com/senirudilmith4/TeamMate-Java-Application
  cd TeamMate-Java-Application
```
 Build the Project

    mvn clean install

  
## 🛣 Future Enhancements

 - JavaFX UI

- Multiple event/sport types

 - Database support (MySQL/PostgreSQL)

 - More personality-driven team scoring


## 👤 Author

- Seniru Dilmith - [@LinkedIN](https://www.linkedin.com/in/senirudilmith/)

 
