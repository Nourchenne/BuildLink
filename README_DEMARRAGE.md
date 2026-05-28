# 🏗️ BuildLink - Plateforme de gestion de projets de construction

## ✅ Corrections effectuées

Toutes les erreurs HTML ont été corrigées dans le fichier `project-form.html` :
- ✅ Suppression de l'espace de noms inutilisé `xmlns:sec`
- ✅ Ajout d'attributs `id` pour tous les champs de formulaire
- ✅ Association correcte des labels avec les champs via l'attribut `for`
- ✅ Conformité aux standards d'accessibilité web

## 🚀 Démarrage rapide

### 1️⃣ Prérequis
- ☕ **Java 17** ou supérieur
- 🗄️ **MySQL** installé et démarré
- 📦 **Maven** (inclus via wrapper mvnw)

### 2️⃣ Configuration de la base de données

Par défaut, l'application se connecte à :
- **URL** : `jdbc:mysql://localhost:3306/buildlink`
- **Utilisateur** : `root`
- **Mot de passe** : `root`

La base de données sera créée automatiquement au premier démarrage.

Pour modifier ces paramètres, éditez `src/main/resources/application.properties`.

### 3️⃣ Lancer l'application

```bash
# Option 1 : Avec Maven wrapper (recommandé)
./mvnw spring-boot:run

# Option 2 : Avec Maven installé
mvn spring-boot:run

# Option 3 : Compiler puis lancer le JAR
mvn clean package -DskipTests
java -jar target/buildlink-0.0.1-SNAPSHOT.jar
```

### 4️⃣ Accéder à l'application

Une fois démarrée, l'application est accessible à :
- 🌐 **URL** : http://localhost:8080
- 🔐 **Connexion** : http://localhost:8080/auth/login
- 📝 **Inscription** : http://localhost:8080/auth/register

## 👥 Rôles disponibles

L'application propose 3 rôles :

### 👤 CLIENT
- Créer des projets de construction
- Sélectionner un architecte
- Suivre l'avancement des projets
- Approuver/refuser les plans
- Passer des commandes de matériaux
- Laisser des avis

### 🏛️ ARCHITECTE
- Recevoir des demandes de projets
- Accepter/refuser des projets
- Créer des plans avec liste de matériaux
- Envoyer les plans aux clients
- Publier des demandes de matériaux
- Consulter les offres des fournisseurs

### 📦 FOURNISSEUR
- Gérer un catalogue de matériaux
- Répondre aux demandes d'architectes
- Soumettre des offres
- Gérer les commandes

## 📋 Fonctionnalités principales

### ✨ Pour les clients
- ➕ Créer un nouveau projet (formulaire corrigé)
- 🔍 Rechercher des architectes
- 📊 Suivre l'avancement des projets
- ✅ Valider les plans et devis
- 🛒 Commander des matériaux
- ⭐ Évaluer les architectes

### ✨ Pour les architectes
- 📥 Gérer les demandes de projets
- 📐 Créer des plans détaillés
- 📝 Lister les matériaux nécessaires
- 🔎 Rechercher des fournisseurs
- 💰 Comparer les offres
- 📊 Tableau de bord de suivi

### ✨ Pour les fournisseurs
- 📦 Gérer le catalogue de produits
- 💼 Répondre aux appels d'offres
- 📈 Suivre les commandes
- 💵 Gérer les factures

## 🔒 Sécurité

- Authentification par email/mot de passe
- Mots de passe chiffrés avec BCrypt
- Contrôle d'accès par rôle (RBAC)
- Protection CSRF activée
- Sessions HTTP sécurisées

## 🛠️ Technologies utilisées

- **Backend** : Spring Boot 3.3.5, Spring Security, Spring Data JPA
- **Frontend** : Thymeleaf, HTML5, CSS3
- **Base de données** : MySQL 8
- **Build** : Maven
- **Java** : 17

## 📁 Structure du projet

```
buildlink/
├── src/main/java/com/buildlink/buildlink/
│   ├── config/          # Configurations (Security, Password, etc.)
│   ├── controller/      # Contrôleurs MVC
│   ├── dto/            # Data Transfer Objects
│   ├── entity/         # Entités JPA
│   ├── repository/     # Repositories Spring Data
│   └── service/        # Services métier
├── src/main/resources/
│   ├── application.properties  # Configuration
│   ├── static/         # CSS, JS, Images
│   └── templates/      # Templates Thymeleaf
│       ├── client/     # Pages client
│       ├── architect/  # Pages architecte
│       ├── supplier/   # Pages fournisseur
│       ├── auth/       # Authentification
│       └── error/      # Pages d'erreur
└── pom.xml            # Configuration Maven
```

## ✅ État du projet

- ✅ **Compilation** : Succès
- ✅ **Erreurs HTML** : Toutes corrigées
- ✅ **Accessibilité** : Conforme aux standards
- ✅ **Validation** : Fonctionnelle
- ✅ **Prêt à l'emploi** : Oui

## 🐛 Dépannage

### L'application ne démarre pas
- Vérifiez que MySQL est démarré
- Vérifiez que le port 8080 est libre
- Vérifiez que Java 17+ est installé : `java -version`

### Erreur de connexion à la base de données
- Vérifiez que MySQL écoute sur le port 3306
- Vérifiez les identifiants dans `application.properties`
- Créez manuellement la base : `CREATE DATABASE buildlink;`

### Port 8080 déjà utilisé
Modifiez le port dans `application.properties` :
```properties
server.port=8081
```

## 📚 Documentation

- Voir `CORRECTIONS_EFFECTUEES.md` pour le détail des corrections
- Voir `FLUX_LIVRAISON_CLIENT.md` pour le flux métier

## 🎉 Prêt à utiliser !

Le projet est maintenant corrigé et fonctionnel. Vous pouvez :
1. Démarrer l'application
2. Créer un compte (CLIENT, ARCHITECTE ou FOURNISSEUR)
3. Explorer les fonctionnalités
4. Créer votre premier projet !

---
*Dernière mise à jour : 28 Mai 2026*

