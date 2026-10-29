# Billing and Invoice Generator

A Java-based medical billing application that validates billing codes, applies payer-specific fee schedules, handles procedure bundling and modifiers, and calculates payer and patient financial responsibility.

## Overview

This project demonstrates a service-based approach to medical billing and invoice generation. It uses Java 21, Maven, and BigDecimal for financial calculations.

## Features

- Models encounters, procedure lines, diagnoses, payers, fee schedules, modifiers, invoices, and adjustments.
- Validates registered CPT and ICD-10 codes against configured validity dates.
- Validates configured CPT-to-ICD-10 diagnosis pairings.
- Selects payer-specific fee schedule rates based on the service date.
- Falls back to hospital standard charges when a matching fee schedule rate is unavailable.
- Supports configurable procedure bundling rules.
- Supports modifier 59 for configured distinct procedural services.
- Applies modifier 50 at 150% of the base allowed amount.
- Applies modifier 51 at 50% for eligible subsequent procedures.
- Calculates contractual write-offs and billing adjustments.
- Calculates payer responsibility, patient responsibility, outstanding balance, and credit balances.
- Uses BigDecimal and payer-configured rounding rules for monetary calculations.
- Includes automated unit, integration, reconciliation, and edge-case tests.

## Technology Stack

- Java 21
- Apache Maven
- Maven Wrapper
- JUnit Jupiter 5
- Git and GitHub

## Prerequisites

- JDK 21
- Git, if cloning the repository
- Internet access for Maven dependency downloads during the first build

A separate Maven installation is not required because the project includes the Maven Wrapper.

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/sunnykotha/billing-invoice-generator.git
cd billing-invoice-generator
```

### 2. Check Java

```powershell
java -version
```

Ensure Java 21 is available.

### 3. Run the tests on Windows

```powershell
.\mvnw.cmd clean test
```

### 4. Package the project on Windows

```powershell
.\mvnw.cmd clean package
```

The generated JAR is placed in the `target` directory.

### macOS and Linux

Run the following commands from the project directory:

```bash
chmod +x mvnw
./mvnw clean test
./mvnw clean package
```

## Billing Workflow

1. **Validate:** Check registered CPT and ICD-10 codes, code validity dates, and configured diagnosis-to-procedure relationships.
2. **Price:** Resolve allowed amounts using the applicable payer fee schedule or hospital standard charge fallback.
3. **Apply bundling and modifiers:** Evaluate configured component bundling rules and applicable modifiers.
4. **Calculate adjustments:** Determine contractual write-offs and record applicable billing adjustments.
5. **Calculate financial responsibility:** Calculate payer and patient shares according to the configured payer share rate.
6. **Reconcile:** Apply prior payments and calculate the outstanding patient balance or credit balance.

## Financial Calculation Considerations

- Contractual write-offs are not added to patient responsibility.
- Payer responsibility uses the configured payer share rate and rounding rules.
- Patient responsibility is calculated from the allowed amount after the payer portion.
- Prior payments reduce the outstanding patient balance.
- Overpayments are represented as credit balances.
- Fee schedule applicability is determined by the encounter service date.
- Bundling behavior depends on configured billing rules.

## Project Structure

```text
billing-invoice-generator/
├── .mvn/
│   └── wrapper/
├── src/
│   ├── main/java/com/billing/
│   │   ├── billing/
│   │   │   ├── BillingCalculationService.java
│   │   │   └── InvoiceGenerationService.java
│   │   ├── model/
│   │   ├── pricing/
│   │   │   ├── BundlingAndModifierService.java
│   │   │   └── FeeSchedulePricingService.java
│   │   └── validation/
│   │       └── BillingValidator.java
│   └── test/java/com/billing/
├── .gitignore
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Testing

The project passed 45 automated tests in the last recorded test run. Tests cover billing calculations, validation, fee schedule pricing, modifiers, bundling, invoice generation, reconciliation, rounding, and overpayment scenarios.

Run the current test suite with:

```powershell
.\mvnw.cmd clean test
```

## Scope and Limitations

This project implements configurable billing rules for learning and demonstration purposes. Code validation depends on the codes and diagnosis pairings registered with the validator. Bundling rules must be configured explicitly.

It is not a substitute for official code-set licensing, payer contract verification, or production healthcare billing compliance review.

## Author

**Sunny Kotha**

GitHub: https://github.com/sunnykotha

## License

No license has been specified. All rights are reserved unless a license is added to this repository.
