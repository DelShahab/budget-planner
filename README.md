# Budget Planner

A modern, responsive budget planning application built with Vaadin Flow and Spring Boot.

## Features

- **Interactive Dashboard**: Real-time budget tracking with visual charts and summaries
- **Category Management**: Organize expenses into Income, Expenses, Bills, and Savings
- **Hierarchical Navigation**: Navigate budgets by year and month with an intuitive tree structure
- **Dynamic Forms**: Context-aware forms for adding and editing budget items
- **Responsive Design**: Modern UI that works on desktop and mobile devices
- **Keyboard Shortcuts**: Efficient navigation with Ctrl+N to add new items
- **Visual Analytics**: Charts showing budget allocation and spending patterns

## Technology Stack

- **Backend**: Spring Boot 3.3.5
- **Frontend**: Vaadin Flow 24.4.4
- **Database**: H2 (development) / PostgreSQL (production)
- **Build Tool**: Maven
- **Java Version**: 17

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Running the Application

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd budget-planner
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

3. Open your browser and navigate to:
   ```
   http://localhost:8080
   ```

### Development Mode

The application runs in development mode by default with:
- Hot reload enabled
- H2 in-memory database
- SQL logging enabled
- H2 console available at `/h2-console`

## Usage

### Navigation
- Use the sidebar tree to navigate between years and months
- Current month is automatically selected on startup
- Expand/collapse years to view monthly budgets

### Managing Budget Items
- **Add new items**: Click the floating "+" button or press Ctrl+N
- **Edit items**: Double-click any budget item in the grids
- **Categories**: Items are organized into Income, Expenses, Bills, and Savings
- **Form behavior**: Category type is automatically set based on which grid you're editing from

### Dashboard Features
- **Summary cards**: Overview of total planned vs actual amounts
- **Visual charts**: Pie charts showing budget allocation and spending distribution
- **Category grids**: Detailed view of all budget items by category
- **Real-time updates**: Dashboard refreshes automatically when items are added/edited

## Project Structure

```
src/main/java/com/budgetplanner/budget/
├── Application.java              # Spring Boot main class
├── BudgetView.java              # Main UI component
├── NavigationNode.java          # Tree navigation model
├── model/
│   └── BudgetItem.java         # Budget item entity
└── repository/
    └── BudgetItemRepository.java # Data access layer

src/main/resources/
├── application.properties       # Configuration
└── frontend/styles/            # CSS styling
```

## Configuration

### Database Configuration

**Development (H2):**
```properties
spring.datasource.url=jdbc:h2:mem:budgetdb
spring.h2.console.enabled=true
```

**Production (PostgreSQL):**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/budgetdb
spring.datasource.username=budget_user
spring.datasource.password=your_password
```

### Vaadin Configuration
```properties
vaadin.allowed-packages=com.budgetplanner.budget
vaadin.launch-browser=false
```

## Building for Production

1. Build the application:
   ```bash
   mvn clean package -Pproduction
   ```

2. Run the JAR file:
   ```bash
   java -jar target/budget-planner-1.0.0.jar
   ```

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details on how to get started.

### Quick Start for Contributors

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes and add tests
4. Commit your changes: `git commit -m 'feat: add amazing feature'`
5. Push to the branch: `git push origin feature/amazing-feature`
6. Open a Pull Request

## Security

If you discover a security vulnerability, please follow our [Security Policy](SECURITY.md) for responsible disclosure.

## Code of Conduct

This project adheres to the Contributor Covenant [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Badges

![CI](https://github.com/DelShahab/budget-planner/workflows/CI/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/java-17+-orange.svg)
![Spring Boot](https://img.shields.io/badge/spring--boot-3.3.5-green.svg)
![Vaadin](https://img.shields.io/badge/vaadin-24.4.4-blue.svg)

## Support

For support or questions:
- 📋 [Open an issue](https://github.com/DelShahab/budget-planner/issues/new/choose)
- 💬 [Start a discussion](https://github.com/DelShahab/budget-planner/discussions)
- 📖 Check the [documentation](README.md)

## Acknowledgments

- [Vaadin](https://vaadin.com/) for the excellent UI framework
- [Spring Boot](https://spring.io/projects/spring-boot) for the robust backend
- All contributors who help improve this project
