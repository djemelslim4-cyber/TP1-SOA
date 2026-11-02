TP1 SOA : Modélisation XML d'un cabinet médical

1. Structure du projet

tp1-cabinet-medical/
├── pom.xml
├── DOCUMENTATION.md
└── src/main/
    ├── java/tn/fss/soa/Main.java      => validation XSD + requêtes XPath
    └── resources/
        ├── cabinet.xsd                 => schéma
        ├── cabinet.xml                 => document valide
        └── cabinet-invalide.xml        => document volontairement invalide 

2. Hiérarchie XML

cabinet
├── informations (nom, adresse, contact)
├── medecins
│   └── medecin [@id]  (nom, prenom, cin, specialite, contact, horaires/horaire[@jour], tarifConsultation)
├── patients
│   └── patient [@id, @assureCNAM]
│       (nom, prenom, cin?, sexe, dateNaissance, adresse, contact,
│        dossierMedical: groupeSanguin, poids, taille, allergies/allergie[@gravite]?, antecedents?)
├── rendezVousListe
│   └── rendezVous [@id, @medecinRef, @patientRef, @statut]  (date, heure, motif)
├── consultations
│   └── consultation [@id, @rdvRef]
│       (constantes: temperature, tension?, poids? ; diagnostic ; ordonnance/medicament+? ; examens?)
└── factures
    └── facture [@id, @consultationRef, @payee]  (montant, modePaiement)

Choix de modélisation
- Profondeur : jusqu'à 5 niveaux (cabinet/patients/patient/dossierMedical/allergies/allergie).
- Éléments conteneurs (medecins, patients, ...) pour regrouper les collections.
- Attributs pour les identifiants, références et statuts (métadonnées) ; éléments pour les données.
- Références par identifiant (medecinRef, patientRef, rdvRef, consultationRef) plutôt que d'imbriquer : évite la duplication, comme des clés étrangères.
- Types réutilisables (AdresseType, ContactType) partagés par le cabinet, les médecins et les patients.
- cin du patient optionnel (un enfant n'en a pas), ordonnance, allergies, etc. aussi optionnels.

3. Conditions personnalisées dans le XSD

| Condition | Mécanisme XSD |
|---|---|
| CIN = exactement 8 chiffres | pattern [0-9]{8} (CinType) |
| Téléphone tunisien (8 chiffres, commence par 2,3,4,5,7,9) | pattern [234579][0-9]{7} |
| Email valide | pattern (EmailType) |
| Identifiants formatés (M001, P0001, RDV0001, C0001, F0001) | pattern par type |
| Groupe sanguin, spécialité, statut RDV, mode de paiement, sexe, jour | enumeration |
| Température entre 34 et 42 °C (1 décimale) | minInclusive`/maxInclusive`/`fractionDigits` |
| Poids 1 à 300 kg, taille 30 à 250 cm | idem |
| Tension au format 120/80 | pattern [0-9]{2,3}/[0-9]{2,3} |
| Montant ≥ 0, 3 décimales (dinar) | minInclusive, fractionDigits |
| Durée de traitement 1 à 365 jours | positiveInteger + maxInclusive |
| Nom sans espace en début/fin, 2 à 50 caractères | minLength, maxLength, pattern |
| Date de naissance ≥ 1900 | minInclusive sur xs:date |
| Gravité d'allergie : légère / modérée / sévère | attribut avec enumeration (simpleContent) |
| 1 à 3 numéros de téléphone, 1 à 6 horaires par médecin | maxOccurs |

Contraintes d'intégrité (au niveau de <cabinet>)
- xs:key : unicité et existence des id de médecins, patients, RDV, consultations.
- xs:keyref : un RDV doit référencer un médecin et un patient existants ; une consultation un RDV existant ; une facture une consultation existante.
- xs:unique : CIN unique par patient et par médecin ; un médecin ne peut pas avoir deux RDV au même jour et à la même heure (creneauUnique) ; une seule consultation par RDV ; une seule facture par consultation.

Limite : la validation se fait en XSD 1.0 (seule version supportée par JAXP du JDK). Les règles inter-champs (ex. date de RDV non passée, tension systolique > diastolique) nécessiteraient xs:assert (XSD 1.1) et sont donc hors périmètre.


4. Fonctionnalité 1 : validation (Main.validate)
SchemaFactory charge cabinet.xsd → Schema → Validator. Un ErrorHandler collecte toutes les erreurs (avec le numéro de ligne) au lieu de s'arrêter à la première.
cabinet-invalide.xml contient 5 violations volontaires (CIN trop courte, téléphone invalide, groupe sanguin C+, température 45 °C, référence vers un patient inexistant P9999), toutes détectées.

5. Fonctionnalité 2 : requêtes XPath (XPathFactory + XPathExpression)
JAXP implémente XPath 1.0 (pas de concat() dans un chemin, par exemple).

| Besoin | Requête |
|---|---|
| Tous les médecins | //medecin/nom |
| Cardiologues | //medecin[specialite='Cardiologie']/nom |
| Patients assurés CNAM | //patient[@assureCNAM='true']/nom |
| Patients avec allergie sévère | //patient[dossierMedical/allergies/allergie/@gravite='severe']/nom |
| RDV confirmés | //rendezVous[@statut='confirme']/motif |
| Patients nés après le 2008-11-02 | //patient[translate(dateNaissance,'-','') > 20081102]/prenom |
| Médicaments prescrits | //ordonnance/medicament/nom |
| Consultations avec fièvre | //consultation[constantes/temperature > 38]/diagnostic |
| Factures impayées | //facture[@payee='false']/@id |
| RDV du patient P0001 | //rendezVous[@patientRef='P0001']/date |
| Nombre de patients | count(//patient) |
| Total des factures impayées | sum(//facture[@payee='false']/montant) |
| Médecin d'un RDV (jointure par référence) | //medecin[@id=//rendezVous[@id='RDV0003']/@medecinRef]/nom |

6. Résultats de l'exécution
- cabinet.xml : DOCUMENT VALIDE
- cabinet-invalide.xml : DOCUMENT INVALIDE (9 messages d'erreur JAXP, avec lignes)
- Toutes les requêtes XPath renvoient les résultats attendus (ex. 3 patients, total des factures 140 TND dont 60 TND impayés).****
