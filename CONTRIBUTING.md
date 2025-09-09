# Contributing to Budget Planner

Thank you for your interest in contributing to Budget Planner! We welcome contributions from the community.

## How to Contribute

### Reporting Issues

Before creating an issue, please:

1. **Search existing issues** to avoid duplicates
2. **Use the issue templates** provided
3. **Provide clear reproduction steps** for bugs
4. **Include relevant system information** (OS, Java version, browser)

### Submitting Changes

1. **Fork the repository**
2. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** following our coding standards
4. **Add tests** for new functionality
5. **Update documentation** if needed
6. **Commit with clear messages**:
   ```bash
   git commit -m "feat: add budget export functionality"
   ```
7. **Push to your fork** and **create a pull request**

### Development Setup

1. **Prerequisites:**
   - Java 17 or higher
   - Maven 3.6 or higher
   - Git

2. **Clone and setup:**
   ```bash
   git clone https://github.com/DelShahab/budget-planner.git
   cd budget-planner
   mvn clean install
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Run tests:**
   ```bash
   mvn test
   ```

### Coding Standards

- **Java Code Style:** Follow standard Java conventions
- **Commit Messages:** Use [Conventional Commits](https://conventionalcommits.org/)
  - `feat:` for new features
  - `fix:` for bug fixes
  - `docs:` for documentation changes
  - `style:` for formatting changes
  - `refactor:` for code refactoring
  - `test:` for adding tests
  - `chore:` for maintenance tasks

### Code Review Process

1. All submissions require review before merging
2. We may suggest changes, improvements, or alternatives
3. Keep discussions respectful and constructive
4. Address feedback promptly

### Testing Guidelines

- Write unit tests for new functionality
- Ensure all tests pass before submitting
- Include integration tests for complex features
- Test UI changes across different browsers

### Documentation

- Update README.md for new features
- Add inline code comments for complex logic
- Update API documentation if applicable
- Include examples for new functionality

## Community Guidelines

### Code of Conduct

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on constructive feedback
- Respect different viewpoints and experiences

### Getting Help

- Check existing documentation first
- Search closed issues for solutions
- Ask questions in issue discussions
- Be patient and respectful when seeking help

## Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes for significant contributions
- GitHub contributor graphs

Thank you for contributing to Budget Planner!
