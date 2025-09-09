# Security Policy

## Supported Versions

We actively support the following versions of Budget Planner:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security vulnerability, please follow these steps:

### Private Disclosure

**DO NOT** create a public GitHub issue for security vulnerabilities.

Instead, please report security vulnerabilities by:

1. **Email**: Send details to [security@budgetplanner.com] (if available)
2. **GitHub Security Advisory**: Use GitHub's private vulnerability reporting feature
3. **Direct Message**: Contact the maintainer directly

### What to Include

When reporting a vulnerability, please include:

- **Description** of the vulnerability
- **Steps to reproduce** the issue
- **Potential impact** assessment
- **Suggested fix** (if you have one)
- **Your contact information** for follow-up

### Response Timeline

- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 7 days
- **Status Updates**: Weekly until resolved
- **Resolution**: Target within 30 days for critical issues

### Security Best Practices

When using Budget Planner:

#### For Development
- Keep dependencies updated
- Use HTTPS in production
- Secure database connections
- Validate all user inputs
- Follow OWASP guidelines

#### For Production Deployment
- Use strong database passwords
- Enable SSL/TLS encryption
- Configure proper firewall rules
- Regular security updates
- Monitor application logs

#### Database Security
- Change default passwords
- Use environment variables for sensitive data
- Enable database encryption
- Regular backups with encryption
- Limit database access permissions

### Security Features

Budget Planner includes:

- **Input Validation**: All user inputs are validated
- **SQL Injection Protection**: Using JPA/Hibernate parameterized queries
- **XSS Protection**: Vaadin framework provides built-in XSS protection
- **CSRF Protection**: Spring Security CSRF tokens
- **Session Management**: Secure session handling

### Vulnerability Disclosure Policy

- We will acknowledge receipt of vulnerability reports
- We will provide regular updates on our progress
- We will credit researchers who responsibly disclose vulnerabilities
- We will publish security advisories for significant vulnerabilities

### Hall of Fame

We recognize security researchers who help improve Budget Planner's security:

*No entries yet - be the first!*

## Contact

For security-related questions or concerns:
- Create a private security advisory on GitHub
- Contact the maintainer through GitHub

Thank you for helping keep Budget Planner secure!
