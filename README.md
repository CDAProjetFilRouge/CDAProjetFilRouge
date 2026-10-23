# MyHubEvent

Plateforme d'évènements d'une fédération régionale de clubs : les membres parcourent le catalogue, s'inscrivent d'un geste et suivent leur place, les organisateurs gèrent leurs évènements, les administrateurs gèrent les clubs et les comptes.

Projet fil rouge de formation (Diginamic), développé en équipe.

![Le catalogue MyHubEvent](docs/screenshots/catalogue.jpg)

## Fonctionnalités

**Pour tous les membres**
- **Catalogue** filtrable par mot-clé, catégorie, club, ville, dates et statut. Chaque évènement est un billet dont la souche affiche le prix (tarif affilié pour les membres d'un club) et dont l'état se lit d'un coup d'œil : inscrit, liste d'attente, complet, annulé.
- **Inscription** depuis le billet ou la page de détail, désinscription, **liste d'attente** FIFO avec rang et promotion automatique (un email prévient la personne promue). Les inscriptions se ferment quand l'évènement est annulé, terminé ou commencé.
- **Tableau de bord** avec des indicateurs adaptés au rôle et un **calendrier** pleine hauteur de ses inscriptions : un clic ouvre le détail, la désinscription et le lien vers l'évènement.
- **Mes évènements passés** : l'historique des évènements auxquels on était inscrit (terminés ou annulés), depuis le menu du compte ou le tableau de bord, avec un lien vers la page de détail (photos et commentaires).
- **Commentaires** sur la page de détail d'un évènement. Sur un évènement terminé, seuls les participants et l'organisateur peuvent commenter ; sur un évènement annulé, seul l'organisateur. L'organisateur peut aussi fermer les commentaires à la fin de l'évènement. Un contrôle de fréquence (5 par minute, 20 par évènement), le refus des doublons et un filtre de contenu (liens, insultes, majuscules, répétitions) limitent le spam. La liste des mots refusés est dans [comment-banned-words.txt](backend/src/main/resources/comment-banned-words.txt).

**Pour les organisateurs**
- Création et gestion des évènements, brouillons, galerie de photos (JPEG, PNG ou WebP), liste des inscrits, inscription retirée avec motif.
- **Cycle de vie** : brouillon → publié → annulé ou terminé. Les transitions sont contrôlées côté serveur (un évènement annulé ou terminé ne repart pas). Un évènement publié passe tout seul en « terminé » quand sa date de fin est atteinte (vérification toutes les 5 minutes), avec la possibilité de le terminer à la main.
- **Annulation** d'un évènement : les inscriptions confirmées sont conservées, la liste d'attente est annulée, et tout le monde reçoit un email.
- **Calendrier** à deux calques, « Mes inscriptions » et « Mes évènements » (tous statuts, brouillons compris), avec le remplissage de chaque évènement.
- Suppression d'un évènement futur : un email prévient les inscrits.

**Pour les administrateurs**
- Clubs, utilisateurs (création, rôle, suspension définitive ou temporaire, suppression), évènements, **demandes d'anonymisation RGPD**, documents légaux versionnés (conditions d'utilisation, politique RGPD), exports PDF.

**Sécurité**
- Mots de passe Argon2id, JWT de courte durée **lié à une clé du navigateur (DPoP, obligatoire)**, refresh token rotatif avec détection de réutilisation, limite progressive des tentatives de connexion, limite de fréquence des requêtes par adresse IP.
- Rôles contrôlés à la fois par URL et sur chaque méthode ; un brouillon, la liste des membres d'un club ou un PDF de brouillon ne sont jamais révélés aux comptes qui n'y ont pas droit.
- Procédure en cas de fuite d'un secret : [docs/procedure-fuite-secrets.md](docs/procedure-fuite-secrets.md).

**Logs**
- Une ligne par requête (méthode, chemin, statut, durée) avec un identifiant de requête `X-Request-Id` repris dans la réponse, l'adresse IP et l'utilisateur. Les événements de sécurité (connexion refusée, jeton rejeté, limite atteinte) et les actions sensibles (changement de statut, suppression, suspension) sont tracés, sans jamais écrire d'email, de mot de passe ni de jeton. En production, les logs sont en JSON sur la sortie standard (`docker compose logs backend`).

## Stack

| Couche | Technologies |
|---|---|
| Back | Java 21, Spring Boot 4, Spring Security, JPA, MariaDB |
| Front | Angular 22, TypeScript, FullCalendar |
| API | OpenAPI / Swagger (`/swagger-ui.html`) |
| Tests | JUnit 5 et Mockito (back, tests unitaires et d'intégration), Vitest (front) |
| CI | GitHub Actions (build et tests du back et du front) |

## Déployer avec Docker

Il faut Docker avec le plugin Compose. Depuis la racine du dépôt :

```bash
cp .env.example .env     # renseigner DB_PASSWORD, DB_ROOT_PASSWORD et JWT_SECRET
docker compose up --build
```

Le front est servi sur `http://localhost:8081` et l'API sur `http://localhost:8080`. Les tables sont créées et mises à jour au démarrage ; la base et les images envoyées sont conservées dans des volumes Docker.

Variables de `.env` :

| Variable | Rôle |
|---|---|
| `DB_PASSWORD`, `DB_ROOT_PASSWORD` | Mots de passe de la base (obligatoires) |
| `JWT_SECRET` | Clé de signature des jetons, chaîne aléatoire longue (obligatoire, `openssl rand -base64 48`) |
| `API_URL` | Adresse publique de l'API, vue par le navigateur, sans `/` final |
| `FRONTEND_URL` | Adresse publique du front, utilisée dans les liens des emails, sans `/` final |
| `BREVO_SMTP_LOGIN`, `BREVO_SMTP_KEY` | Envoi des emails via Brevo. Sans eux, l'application tourne mais n'envoie rien |

`API_URL` et `FRONTEND_URL` sont lues au démarrage : il n'y a pas d'image à reconstruire pour changer d'adresse.

Pour charger le jeu de démonstration (mot de passe des comptes : `Password12345!`) :

```bash
docker compose exec -T db mariadb -uroot -p<DB_ROOT_PASSWORD> hub_evenementiel < docs/seed-data.sql
```

### Mise en production : HTTPS

Le `docker-compose.yml` expose l'API et le front en HTTP. Sur un vrai serveur, il faut un **proxy qui gère le HTTPS** (nginx avec Certbot, Caddy, ou le HTTPS fourni par l'hébergeur), et mettre les adresses `https://…` dans `API_URL` et `FRONTEND_URL`.

Deux points comptent pour l'API :
- Le proxy doit transmettre l'adresse d'origine : le navigateur signe ses requêtes (DPoP) avec l'adresse `https://…` qu'il a appelée, et les limites de fréquence lisent l'adresse IP du visiteur.
- L'API ne doit être joignable que par le proxy (écouter sur `127.0.0.1`), sinon `X-Forwarded-For` peut être falsifié pour contourner les limites.

Exemple de bloc nginx pour l'API :

```nginx
server {
    server_name api.exemple.fr;
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
    }
    listen 443 ssl;   # certificat géré par Certbot
}
```

Dans le `docker-compose.yml`, remplacer `"8080:8080"` par `"127.0.0.1:8080:8080"` (et `"8081:80"` par `"127.0.0.1:8081:80"`) pour que seuls les proxys y accèdent.

### Sauvegarde

La base et les images sont dans des volumes Docker : rien ne les sauvegarde tout seul.

```bash
# base de données
docker compose exec -T db mariadb-dump -uroot -p<DB_ROOT_PASSWORD> hub_evenementiel > sauvegarde-$(date +%F).sql

# images envoyées
docker compose cp backend:/data/uploads ./sauvegarde-images-$(date +%F)
```

À planifier (cron) et à copier hors du serveur. Pour restaurer la base : `docker compose exec -T db mariadb -uroot -p<DB_ROOT_PASSWORD> hub_evenementiel < sauvegarde.sql`.

## Lancer en local

**Back** (`backend/`) : copier `.env.example` en `.env` et renseigner les valeurs (base MariaDB, `JWT_SECRET`, identifiants SMTP), exporter ces variables, puis :

```bash
cd backend
mvn spring-boot:run
```

L'API écoute sur `http://localhost:8080`. Les tables sont créées au démarrage avec le profil `dev`, qui écrit les logs en texte lisible. Le script [docs/seed-data.sql](docs/seed-data.sql) charge un jeu de données de démonstration.

**Front** (`frontend/`) :

```bash
cd frontend
npm install
npx ng serve
```

L'application est servie sur `http://localhost:4200` et appelle l'API sur `http://localhost:8080`. Pour changer cette adresse, éditer `frontend/public/config.js` (`window.APP_CONFIG = { apiUrl: '...' };`).

## Tests

```bash
cd backend && mvn test      # nécessite une base MariaDB (variables SPRING_DATASOURCE_*)
cd frontend && npm test
```

Les tests d'intégration du back utilisent une base dédiée (jamais la base de développement) : créer une base vide et la désigner avec `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` et `SPRING_DATASOURCE_PASSWORD`, ainsi que `JWT_SECRET`, `BREVO_SMTP_LOGIN` et `BREVO_SMTP_KEY` (les valeurs peuvent être fictives pour les tests). La CI les exécute à chaque PR.

## Documentation

Le dossier [docs/](docs/) contient la phase de conception : [backlog](docs/backlog.md) (user stories, règles de gestion), diagrammes (classes, modèle physique, cas d'utilisation), maquettes, dossier de conception et dossier de spécifications générales.
