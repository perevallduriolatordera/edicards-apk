# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application for Edicards, a business management system for handling deposits, clients, articles, and inventory. The app targets Android API 26 and is written in Java using legacy Android Support libraries.

## Build Commands

### Development Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Clean Build
```bash
./gradlew clean
```

### Install Debug APK
```bash
./gradlew installDebug
```

### Generate Signed APK
Use Android Studio's Build > Generate Signed Bundle/APK option as the project uses legacy signing configuration.

## Project Architecture

### Core Components

#### Application Layer
- `AppConfig.java` - Main application class extending Android Application
- Manages global services: database, cache, connectivity, user session
- Initializes UCE (Uncaught Exception) handler and AndroidNetworking library

#### Data Layer
- `DatabaseOperations.java` - SQLite database operations with 18+ tables
- `CacheData.java` - In-memory caching system
- Data transfer objects in `DataTier/` package (Cliente, Articulo, Deposito, etc.)

#### Service Layer
- `ServiceWorker.java` - Background service orchestration
- `RestClient.java` - HTTP client for web service communication
- `HttpService.java` - HTTP service abstraction
- Web service endpoints defined in `ConstantsEndpoints.java`

#### Business Logic
- Activity-based screens for different business functions:
  - `DepositManager.java` - Deposit management
  - `CustomerSearch.java` - Customer search and selection
  - `StockManager.java` - Inventory management
  - `GastosManager.java` - Expense management
  - `Reports.java` - Various reporting functions

#### UI Layer
- Fragment-based architecture with `MainMenuFragments.java`
- Custom controls in `net.ifeu.library.Controls/` package
- Print functionality with multiple printer support (Star, Woosim)

### Key Constants Files

All constants are organized in `net.ifeu.edicards.Constants/`:
- `ConstantsDatabase.java` - Database table names and structure
- `ConstantsEndpoints.java` - Web service endpoints
- `ConstantsFolders.java` - File system organization
- `ConstantsMail.java` - Email configuration
- `ConstantsFTP.java` - FTP server settings
- `ConstantsCredentials.java` - Authentication credentials
- `ConstantsFirecloud.java` - Firebase configuration

### Database Schema

The app uses SQLite with these main tables:
- `Clientes` - Customer data
- `Articulos` - Product/article information
- `Depositos` - Deposit transactions
- `LineasDeposito` - Deposit line items
- `MovimientosAlmacen` - Stock movements
- `Historicos` - Transaction history
- `Gastos` - Expenses
- `Efectivo` - Cash management
- `LogBook` - Activity logging

### External Integrations

- **Web Services**: Multiple REST endpoints for data synchronization
- **FTP**: File transfer for documents and backups
- **Email**: Multiple SMTP configurations for notifications
- **Firebase**: Cloud storage and messaging
- **Printing**: Star and Woosim thermal printers
- **Barcode**: ZXing library for barcode scanning

## Development Notes

### Key Patterns

1. **Database Operations**: All database access goes through `DatabaseOperations.java`
2. **Service Communication**: Web service calls are handled via `ServiceWorker.java`
3. **Error Handling**: Custom exception handling with `UCEHandler`
4. **Caching**: In-memory cache for frequently accessed data
5. **Logging**: Activity logging through `LogBook` system

### Critical Files for Modifications

- `AppConfig.java` - For application-level changes
- `DatabaseOperations.java` - For database schema changes
- `ServiceWorker.java` - For service integration changes
- `MainActivity.java` - For app initialization flow
- `MainMenu.java` - For main menu functionality

### Testing

The project does not include unit tests. Testing is done through manual testing and device deployment.

### Security Considerations

- Hardcoded credentials are present in Constants files
- Database contains sensitive business data
- FTP and email credentials are stored in plain text
- User authentication is handled through web service calls

### Current Branch Context

Working on `features/cash-control` branch which involves cash management functionality. The `Efectivo` table and related components are likely areas of focus for cash control features.

## Common File Locations

- Activities: `app/src/main/java/net/ifeu/edicards/`
- Data models: `app/src/main/java/net/ifeu/edicards/DataTier/`
- Constants: `app/src/main/java/net/ifeu/edicards/Constants/`
- Services: `app/src/main/java/net/ifeu/edicards/Services/`
- Library code: `app/src/main/java/net/ifeu/library/`
- Resources: `app/src/main/res/`
- Layouts: `app/src/main/res/layout/`