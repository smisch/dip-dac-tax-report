import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

public class XmlValidator {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java XmlValidator <xmlFile> <xsdFile>");
            return;
        }

        String xmlFilePath = args[0];
        String xsdFilePath = args[1];

        File xmlFile = new File(xmlFilePath);
        File xsdFile = new File(xsdFilePath);

        if (!xmlFile.exists()) {
            System.err.println("XML file does not exist.");
            return;
        }

        if (!xsdFile.exists()) {
            System.err.println("XSD file does not exist.");
            return;
        }

        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(xsdFile);
            Validator validator = schema.newValidator();

            Source source = new StreamSource(xmlFile);
            validator.validate(source);

            System.out.println("XML is valid according to the schema.");
        } catch (SAXException e) {
            System.err.println("Validation error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}