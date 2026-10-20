# MyHubEvent

Plateforme d'évènements d'une fédération régionale de clubs : les membres parcourent le catalogue, s'inscrivent d'un geste et suivent leur place, les organisateurs gèrent leurs évènements, les administrateurs gèrent les clubs et les comptes.

Projet fil rouge de formation (Diginamic), développé en équipe.

![Le catalogue MyHubEvent](docs/screenshots/catalogue.jpg)

## Fonctionnalités

- **Catalogue** filtrable par mot-clé, catégorie, club, ville, dates et statut. Chaque évènement est un billet dont la souche affiche le prix (tarif affilié pour les membres d'un club) et dont l'état se lit d'un coup d'œil : inscrit, liste d'attente, complet, annulé.
- **Inscription** depuis le billet ou la page de détail, désinscription, **liste d'attente** FIFO avec rang.
- **Calendrier** et « Mes évènements ».
- **Espace organisateur** : création et gestion des évènements, brouillons, liste des inscrits.
- **Administration** : clubs, utilisateurs, documents légaux versionnés (conditions d'utilisation, RGPD).
- **Sécurité** : mots de passe Argon2id, JWT de courte durée lié à une clé du navigateur (DPoP), refresh token rotatif avec détection de réutilisation, limite progressive des tentatives de connexion.

## Stack

| Couche | Technologies |
|---|---|
| Back | Java 21, Spring Boot 4, Spring Security, JPA, MariaDB |
| Front | Angular 22, TypeScript |
| API | OpenAPI / Swagger (`/swagger-ui.html`) |
| Tests | JUnit 5 et Mockito (back), Vitest (front) |
| CI | GitHub Actions |

## Lancer en local

**Back** (`backend/`) : copier `.env.example` en `.env` et renseigner les valeurs (base MariaDB, `JWT_SECRET`, identifiants SMTP), exporter ces variables, puis :

```bash
cd backend
mvn spring-boot:run
```

L'API écoute sur `http://localhost:8080`. Les tables sont créées au démarrage avec le profil `dev`. Le script [docs/seed-data.sql](docs/seed-data.sql) charge un jeu de données de démonstration.

**Front** (`frontend/`) :

```bash
cd frontend
npm install
npx ng serve
```

L'application est servie sur `http://localhost:4200`.

## Tests

```bash
cd backend && mvn test      # nécessite une base MariaDB ou MySQL (variables SPRING_DATASOURCE_*)
cd frontend && npm test
```

## Documentation

Le dossier [docs/](docs/) contient la phase de conception : [backlog](docs/backlog.md) (user stories, règles de gestion), diagrammes (classes, modèle physique, cas d'utilisation), maquettes, dossier de conception et dossier de spécifications générales.
