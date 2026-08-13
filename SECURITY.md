# Security Policy — BioGuard Wear OS

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

The BioGuard engineering team takes security vulnerabilities seriously. If you discover a security issue affecting the BioGuard Wear OS application or its communication protocols, please follow these guidelines:

1. **Do NOT open a public GitHub issue** for security vulnerabilities.
2. Email your findings to **security@bioguard.example.com** or contact the Lead DevSecOps Engineer directly.
3. Include details of the vulnerability, steps to reproduce, and any proof-of-concept code.
4. The team will acknowledge receipt of your report within **24 hours** and provide a resolution timeline within **5 business days**.

## Security & Governance Guidelines

- **Zero Network Surface**: The Wear OS app operates under zero-network surface policies (`usesCleartextTraffic="false"`, no `INTERNET` permission). Telemetry is strictly routed via local encrypted Wear Data Layer channels.
- **Biometric Privacy**: All PII and health telemetry (BPM, Weight, Height, BMI) must be stripped from system logs in production releases (`if (BuildConfig.DEBUG)` wrappers).
- **Pairing Hardening**: Mandatory single-use challenge QR nonces and ECDSA P-256 signatures are enforced on all node pairing operations.
- **Secret Management**: Signing credentials (`BIOGUARD_*`) are managed exclusively via encrypted GitHub Environment Secrets and rotated periodically.
