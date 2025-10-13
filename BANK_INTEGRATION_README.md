# Bank Integration and Data Import

This document describes the bank integration functionality that allows users to securely link their bank accounts and automatically import transactions using Plaid.

## Features

### 🏦 Bank Account Integration
- **Secure Connection**: Uses Plaid Link to securely connect to over 11,000 financial institutions
- **Multiple Accounts**: Support for linking multiple bank accounts from different institutions
- **Account Management**: View, sync, and remove connected accounts through a dedicated management interface

### 📊 Automatic Transaction Import
- **Real-time Sync**: Import transactions from connected bank accounts
- **Historical Data**: Access up to 24 months of transaction history (depending on institution)
- **Duplicate Prevention**: Automatic detection and prevention of duplicate transactions

### 🤖 Smart Categorization
- **AI-Powered**: Automatic categorization of transactions into budget categories (Income, Expenses, Bills, Savings)
- **Merchant Recognition**: Advanced pattern matching for common merchants and transaction types
- **Manual Override**: Ability to manually review and adjust categorizations

### 🔄 Budget Integration
- **Automatic Updates**: Budget items are automatically updated with actual amounts from bank transactions
- **Category Mapping**: Transactions are mapped to existing budget categories or create new ones
- **Monthly Reconciliation**: Automatic reconciliation of planned vs actual amounts

## Technical Implementation

### Architecture
```
Frontend (Vaadin) → REST API → Service Layer → Plaid API
                              ↓
                         Database (JPA/Hibernate)
```

### Key Components

#### Models
- `BankAccount`: Represents a connected bank account
- `BankTransaction`: Individual transaction from bank
- `BudgetItem`: Existing budget item model (enhanced)

#### Services
- `PlaidService`: Handles all Plaid API interactions
- `BankAccountService`: Manages bank account operations
- `TransactionCategorizationService`: Automatic transaction categorization

#### Security
- Spring Security configuration for API endpoints
- Encrypted storage of access tokens
- CSRF protection for sensitive operations

### Database Schema

#### Bank Accounts Table
```sql
CREATE TABLE bank_accounts (
    id BIGINT PRIMARY KEY,
    plaid_account_id VARCHAR(255) UNIQUE NOT NULL,
    plaid_item_id VARCHAR(255) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    mask VARCHAR(10) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    last_sync_at TIMESTAMP,
    access_token TEXT -- Encrypted in production
);
```

#### Bank Transactions Table
```sql
CREATE TABLE bank_transactions (
    id BIGINT PRIMARY KEY,
    plaid_transaction_id VARCHAR(255) UNIQUE NOT NULL,
    bank_account_id BIGINT REFERENCES bank_accounts(id),
    amount DECIMAL(10,2) NOT NULL,
    merchant_name VARCHAR(255),
    description TEXT,
    transaction_date DATE NOT NULL,
    authorized_date DATE NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    plaid_category VARCHAR(255),
    plaid_subcategory VARCHAR(255),
    budget_category VARCHAR(255),
    budget_category_type VARCHAR(50),
    is_processed BOOLEAN DEFAULT false,
    is_manually_reviewed BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

## Setup Instructions

### 1. Plaid Account Setup
1. Sign up for a Plaid developer account at https://plaid.com/
2. Create a new application in the Plaid Dashboard
3. Get your Client ID and Secret Key
4. Configure webhook endpoints (optional)

### 2. Environment Configuration
Set the following environment variables or update `application.properties`:

```properties
# Plaid Configuration
plaid.client-id=your_plaid_client_id
plaid.secret=your_plaid_secret
plaid.environment=sandbox  # or development/production
```

### 3. Database Setup
The application uses H2 for development and supports PostgreSQL for production:

```properties
# Development (H2)
spring.datasource.url=jdbc:h2:mem:budgetdb
spring.datasource.username=sa
spring.datasource.password=

# Production (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/budgetdb
spring.datasource.username=budget_user
spring.datasource.password=your_secure_password
```

## Usage Guide

### Connecting Bank Accounts

1. **Access Bank Management**
   - Click the user menu (three dots) in the sidebar
   - Select "Manage Bank Accounts"

2. **Link New Account**
   - Click "Link Bank Account" button
   - Complete Plaid Link flow
   - Select your financial institution
   - Enter your online banking credentials
   - Choose accounts to connect

3. **Manage Connected Accounts**
   - View all connected accounts
   - Sync individual accounts
   - Remove accounts when needed

### Transaction Import and Categorization

1. **Automatic Sync**
   - Transactions are automatically imported when accounts are linked
   - Manual sync available through "Sync Transactions" button

2. **Review Categorizations**
   - Check automatically categorized transactions
   - Manually adjust categories as needed
   - System learns from manual adjustments

3. **Budget Integration**
   - Actual amounts in budget are updated automatically
   - View variance between planned and actual spending
   - Monthly reconciliation reports

## API Endpoints

### Bank Integration API
```
POST /api/bank/link-token          # Create Plaid Link token
POST /api/bank/exchange-token      # Exchange public token for access token
POST /api/bank/sync-transactions   # Sync all transactions
DELETE /api/bank/accounts/{id}     # Remove bank account
```

## Security Considerations

### Data Protection
- Access tokens are encrypted at rest
- All API communications use HTTPS
- Plaid handles sensitive banking credentials
- No raw banking credentials stored locally

### Compliance
- PCI DSS compliance through Plaid
- Bank-level security standards
- Regular security audits and updates
- GDPR compliance for EU users

### Best Practices
- Regular token rotation
- Audit logging for all bank operations
- Rate limiting on API endpoints
- Secure session management

## Troubleshooting

### Common Issues

1. **Connection Failures**
   - Verify Plaid credentials
   - Check network connectivity
   - Ensure institution supports Plaid

2. **Sync Issues**
   - Check account status in Plaid Dashboard
   - Verify access token validity
   - Review error logs

3. **Categorization Problems**
   - Review merchant name patterns
   - Check category mapping rules
   - Manual categorization as fallback

### Error Handling
- Graceful degradation when Plaid is unavailable
- User-friendly error messages
- Automatic retry mechanisms
- Comprehensive logging

## Future Enhancements

### Planned Features
- **Real-time Webhooks**: Instant transaction notifications
- **Advanced Analytics**: Spending pattern analysis
- **Goal Tracking**: Automatic progress tracking towards financial goals
- **Multi-currency Support**: International account support
- **Mobile App**: Native mobile application
- **Investment Tracking**: Portfolio and investment account integration

### Performance Optimizations
- Background transaction processing
- Caching for frequently accessed data
- Batch processing for large datasets
- Database indexing optimization

## Support

For technical support or questions:
- Check the application logs for detailed error information
- Review Plaid documentation: https://plaid.com/docs/
- Contact support team with specific error messages and steps to reproduce

## License and Compliance

This implementation follows:
- Plaid Terms of Service
- Financial data handling regulations
- Open source licensing requirements
- Industry security standards
