package tn.fss.soa;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class Main {

    private static final String XSD = "/cabinet.xsd";
    private static final String XML_VALIDE = "/cabinet.xml";
    private static final String XML_INVALIDE = "/cabinet-invalide.xml";

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. VALIDATION XML / XSD ===");
        validate(XML_VALIDE);
        validate(XML_INVALIDE);

        System.out.println("\n=== 2. INTERROGATION XPATH ===");
        Document doc = parse(XML_VALIDE);
        XPath xpath = XPathFactory.newInstance().newXPath();

        afficherListe("Q1. Noms de tous les medecins",
                xpath, doc, "//medecin/nom");
        afficherListe("Q2. Cardiologues",
                xpath, doc, "//medecin[specialite='Cardiologie']/nom");
        afficherListe("Q3. Patients assures CNAM (nom)",
                xpath, doc, "//patient[@assureCNAM='true']/nom");
        afficherListe("Q4. Patients ayant une allergie severe",
                xpath, doc, "//patient[dossierMedical/allergies/allergie/@gravite='severe']/nom");
        afficherListe("Q5. Rendez-vous confirmes (motif)",
                xpath, doc, "//rendezVous[@statut='confirme']/motif");
        afficherListe("Q6. Patients de moins de 18 ans (naissance apres 2008-11-02)",
                xpath, doc, "//patient[translate(dateNaissance,'-','') > 20081102]/prenom");
        afficherListe("Q7. Medicaments prescrits (nom)",
                xpath, doc, "//ordonnance/medicament/nom");
        afficherListe("Q8. Consultations avec fievre (temperature > 38)",
                xpath, doc, "//consultation[constantes/temperature > 38]/diagnostic");
        afficherListe("Q9. Factures impayees (id)",
                xpath, doc, "//facture[@payee='false']/@id");
        afficherListe("Q10. Rendez-vous du patient P0001 (date)",
                xpath, doc, "//rendezVous[@patientRef='P0001']/date");

        // Requetes retournant un nombre / une chaine
        afficherValeur("Nombre de patients", xpath, doc, "count(//patient)", XPathConstants.NUMBER);
        afficherValeur("Total des factures (TND)", xpath, doc, "sum(//facture/montant)", XPathConstants.NUMBER);
        afficherValeur("Total des factures impayees (TND)", xpath, doc,
                "sum(//facture[@payee='false']/montant)", XPathConstants.NUMBER);
        afficherValeur("Medecin du RDV0003", xpath, doc,
                "//medecin[@id=//rendezVous[@id='RDV0003']/@medecinRef]/nom", XPathConstants.STRING);


        XPathExpression expr = xpath.compile("//patient[@id='P0002']/dossierMedical/allergies/allergie");
        NodeList allergies = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        System.out.println("\nAllergies de P0002 (XPathExpression precompilee) :");
        for (int i = 0; i < allergies.getLength(); i++) {
            Node n = allergies.item(i);
            System.out.println("  - " + n.getTextContent()
                    + " (gravite : " + n.getAttributes().getNamedItem("gravite").getNodeValue() + ")");
        }
    }


    static boolean validate(String xmlResource) {
        System.out.println("\nValidation de " + xmlResource + " ...");
        final List<String> erreurs = new ArrayList<>();
        try (InputStream xsd = Main.class.getResourceAsStream(XSD);
             InputStream xml = Main.class.getResourceAsStream(xmlResource)) {

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsd));
            Validator validator = schema.newValidator();


            validator.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) { erreurs.add("[WARN] " + format(e)); }
                public void error(SAXParseException e) { erreurs.add("[ERREUR] " + format(e)); }
                public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });
            validator.validate(new StreamSource(xml));
        } catch (SAXException e) {
            erreurs.add("[FATAL] " + e.getMessage());
        } catch (Exception e) {
            erreurs.add("[IO] " + e.getMessage());
        }

        if (erreurs.isEmpty()) {
            System.out.println("  -> DOCUMENT VALIDE");
            return true;
        }
        System.out.println("  -> DOCUMENT INVALIDE (" + erreurs.size() + " erreur(s)) :");
        erreurs.forEach(e -> System.out.println("     " + e));
        return false;
    }

    private static String format(SAXParseException e) {
        return "ligne " + e.getLineNumber() + " : " + e.getMessage();
    }


    static Document parse(String xmlResource) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        try (InputStream in = Main.class.getResourceAsStream(xmlResource)) {
            return db.parse(in);
        }
    }

    static void afficherListe(String titre, XPath xpath, Document doc, String requete) throws Exception {
        System.out.println("\n" + titre + "\n  XPath : " + requete);
        XPathExpression expr = xpath.compile(requete);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        if (nodes.getLength() == 0) {
            System.out.println("  (aucun resultat)");
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            System.out.println("  - " + nodes.item(i).getTextContent().trim());
        }
    }

    static void afficherValeur(String titre, XPath xpath, Document doc, String requete,
                               javax.xml.namespace.QName type) throws Exception {
        Object r = xpath.compile(requete).evaluate(doc, type);
        System.out.println("\n" + titre + " : " + r + "\n  XPath : " + requete);
    }
}