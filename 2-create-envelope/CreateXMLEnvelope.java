import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import java.security.Key;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

class CreateXMLEnvelope {

	static final String PRIVATE_KEY_PATH = "./private_key.pem";
	static final String CERTIFICATE_PATH = "./certificate.pem";

	static final String INPUT_XML = "./dip.xml";
	static final String OUTPUT_XML = "./dip-signed.xml";

	public static RSAPublicKey readX509PublicKey(File file) throws Exception {
		String key = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());

		String publicKeyPEM = key
			.replace("-----BEGIN PUBLIC KEY-----", "")
			.replaceAll(System.lineSeparator(), "")
			.replace("-----END PUBLIC KEY-----", "");

		byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
		return (RSAPublicKey) keyFactory.generatePublic(keySpec);
	}

    public static Key loadPrivateKey(String filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filePath));

		String key = new String(keyBytes, Charset.defaultCharset());

		String privateKeyPEM = key
		.replace("-----BEGIN PRIVATE KEY-----", "")
		.replaceAll(System.lineSeparator(), "")
		.replace("-----END PRIVATE KEY-----", "");

		byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
		return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

	public static byte[] loadXML(String filePath) throws Exception {
        return Files.readAllBytes(Paths.get(filePath));
    }

	static Key getPrivateKey() throws Exception {
		return loadPrivateKey(PRIVATE_KEY_PATH);
	}

	static KeyInfo createKeyInfo (XMLSignatureFactory xmlsignature) throws Exception{
		// Load the certificate from the PEM file
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        FileInputStream fis = new FileInputStream(CERTIFICATE_PATH);
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(fis);
		
        KeyInfoFactory keyInfoFactory = xmlsignature.getKeyInfoFactory();

		// add the certificate to the output xml
		ArrayList<Object> x509DataList = new ArrayList<Object>();
		x509DataList.add(cert.getSubjectX500Principal().getName());
		x509DataList.add(cert);
		X509Data x509Data = keyInfoFactory.newX509Data(x509DataList);

		ArrayList<XMLStructure> xmlStructures = new ArrayList<XMLStructure>();
		xmlStructures.add(x509Data);

        KeyInfo keyInfoContent = keyInfoFactory.newKeyInfo(xmlStructures);
        return keyInfoContent;
	}

	public static void main(String[] args) throws Exception {
		byte[] dipXml = loadXML(INPUT_XML);

		InputStream unsignedDipStream = new ByteArrayInputStream(dipXml);
		String digestMethod = DigestMethod.SHA256;
		String signatureMethod = SignatureMethod.SHA256_RSA_MGF1;

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		Document dipXmlDocument = documentBuilderFactory.newDocumentBuilder().parse(unsignedDipStream);
		Document newDocument = documentBuilderFactory.newDocumentBuilder().newDocument();
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");
		DOMSignContext domSignContext = new DOMSignContext(getPrivateKey(), newDocument);
		domSignContext.setDefaultNamespacePrefix("ds");
		Reference reference = xmlSignatureFactory.newReference("#object", xmlSignatureFactory.newDigestMethod(digestMethod, null), List.of(), null, null);
		DOMStructure content = new DOMStructure(dipXmlDocument.getDocumentElement());
		XMLObject signedObject = xmlSignatureFactory.newXMLObject(Collections.singletonList(content), "object", null, null);
		SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(
			xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
			xmlSignatureFactory.newSignatureMethod(signatureMethod, null), Collections.singletonList(reference));
		KeyInfo keyInfo = createKeyInfo(xmlSignatureFactory);
		XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, Collections.singletonList(signedObject), null, null);
		xmlSignature.sign(domSignContext);
		Source xmlSource = new DOMSource(newDocument);
		FileOutputStream outputStream = new FileOutputStream(OUTPUT_XML);
		Result outputTarget = new StreamResult(outputStream);
		TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
	}

}
