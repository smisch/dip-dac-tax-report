# What is this?

The German government requires us to exchange some tax related xml files with one of their systems, starting 2024. They provided us with some documentation. We implemented some tools to help with this exchange.

## 1. Getting Key-Pair and Certificate

In order to exchange xml files we first have to provide them with a certificate. We used the `1-create-key-pair/keygen.sh` script to generate key pairs and certificates. The certificate is then uploaded in the government's portal and manually verified. They provide us with a `client_id` in response, usually via email.

Before running the script, be sure to adjust the `subj` argument with your company's infos.

```bash
chmod u+x ./1-create-key-pair/keygen.sh
bash ./1-create-key-pair/keygen.sh
```

## 2. Creating a signed XML document containing the DIP data

We then take the XML document we are trying to send the government, create some signatures from it and envelope it into another XML document for transport. This is done by using the java application in `2-create-envelope`.

Verify that:
```bash
# you have at least java 10
$ javac -version
javac 11.0.22

# you copied your dip.xml here (see path in java file)
$ ls dip.xml
dip.xml
```

### Validate DIP XML

Feel free to validate your DIP XML files against their XML Schema:

```
javac 2a-xml-schema-validator/XmlValidator.java
java -cp 2a-xml-schema-validator XmlValidator dip.xml 2a-xml-schema-validator/dip.xsd
```

### Create envelope

To create envelope, compile java and run:

```
javac 2-create-envelope/CreateXMLEnvelope.java
java -cp 2-create-envelope CreateXMLEnvelope
```

## Sending the document to their API

Finally, we send this xml envelope file to their DAC API using the nodejs scripts in `3-send-envelope`.

```bash
cd 3-send-envelope
npm ci
```

Open `index.mjs`, find `YOUR_CLIENT_ID` and set this to the `client_id` you received via email after uploading your certificate. It should look like a uuidv4, e.g. `8274a2b8-06f6-498d-98f1-48141792dd65`.

Run the file to upload `dip-signed.xml`:

```
node index.mjs
```

Towards the end, if the upload was successful, you'll see a data transfer number, e.g. `ektu0q141b3p5uc4l4kg`. Copy this. Open `index.mjs`, find `EXISTING_DATA_TRANSFER_NUMBER` and assign it there. Run the script again to read the protocol status of this data transfer. Usually, it takes a few seconds for each file to be processed.

## Common upload issues

### don't reuse `<dip:transferticketId>`

Whenever you uploaded a dip file and it fails validation, you can't upload it again with a small change. You have to assign a new `<dip:transferticketId>`, e.g. just add +1.

### use correct `<dip:identifier>`

Check that your `<dip:identifier>BZ123456789</identifier>` is correct and the same as you configured in their portal where you uploaded the certificate.

### switch environment in your dip xml file

When switching from test to production system, make sure you use the correct `<dip:header environment="PROD">` instead of `<dip:header environment="TEST">`.
