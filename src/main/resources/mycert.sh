#!/bin/bash

# Define variables
KEY_NAME="key.pem"
CSR_NAME="request.csr"
CERT_NAME="certificate.pem"
P12_NAME="mycert.p12"
P12_PASSWORD="yourpassword"  # Change this to your desired password

# Step 1: Generate a Private Key
openssl genrsa -out $KEY_NAME 2048

# Step 2: Create a Certificate Signing Request (CSR)
openssl req -new -key $KEY_NAME -out $CSR_NAME -subj "/C=US/ST=State/L=City/O=Organization/CN=www.example.com"

# Step 3: Generate a Self-Signed Certificate
openssl x509 -req -days 365 -in $CSR_NAME -signkey $KEY_NAME -out $CERT_NAME

# Step 4: Create a PKCS#12 File
openssl pkcs12 -export -out $P12_NAME -inkey $KEY_NAME -in $CERT_NAME -name "mycert" -password pass:$P12_PASSWORD

# Cleanup temporary files
rm -f $KEY_NAME $CSR_NAME $CERT_NAME

echo "PKCS#12 file generated and cleanup complete. File: $P12_NAME"