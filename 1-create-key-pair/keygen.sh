#!/bin/bash

# generate private key in RSA format (no RSA-PSS stuff)
openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:4096

# generate certificate with RSA as public key algorithm and RSA-PSS as signing algorithm 
openssl req -x509 -new -key private_key.pem -out certificate.pem -days 36500 -sha256 \
    -sigopt rsa_padding_mode:pss \
    -sigopt rsa_pss_saltlen:32 \
    -subj "/C=DE/ST=Bavaria/L=Nuremberg/O=ACME/OU=Accounting and Finance/CN=www.example.com/emailAddress=info@example.com"

# generate the public key from certificate
openssl x509 -pubkey -noout -in certificate.pem > public_key.pem

# or from private key
# openssl rsa -pubout -in private_key.pem -out public_key.pem

# # look into certificate
# openssl x509 -in certificate.pem -text

# # look into public and private key files
# openssl asn1parse -in public_key.pem
# openssl asn1parse -in private_key.pem
