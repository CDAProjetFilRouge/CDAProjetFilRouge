# Procedure en cas de fuite d'un secret

Si un secret (mot de passe, cle, token) se retrouve visible quelque part ou il
ne devrait pas etre (commit Git, message Slack/Discord, capture d'ecran, etc.),
l'ordre des actions est toujours le meme :

1. **Regenerer/changer la vraie valeur immediatement**, a la source. Retirer
   le secret du code ne l'invalide pas : quelqu'un a pu le copier avant.
   Le considerer comme compromis pour toujours des l'instant ou il a ete visible.
2. **Verifier les usages suspects** pendant la periode d'exposition (logs de
   connexion, activite inhabituelle) si l'outil le permet.
3. **Mettre a jour la nouvelle valeur** partout ou l'ancienne etait utilisee
   (fichier `.env` local de chacun, variables d'environnement du serveur).
4. **Prevenir l'equipe** (et le formateur/l'entreprise si pertinent) - ne pas
   minimiser ni cacher l'incident.
5. Seulement apres les 4 points precedents : corriger le code si la fuite
   venait d'une valeur par defaut codee en dur (comme on l'a fait pour le
   secret JWT, v1.0.1).

## Secrets de ce projet et ou les regenerer

| Secret | Variable | Ou le regenerer |
|---|---|---|
| Mot de passe base de donnees | `SPRING_DATASOURCE_PASSWORD` | Cote MariaDB : changer le mot de passe de l'utilisateur applicatif |
| Cle de signature JWT | `JWT_SECRET` | Generer une nouvelle chaine aleatoire (ex: `openssl rand -base64 48`). Tous les tokens deja emis deviennent invalides des le changement - normal, c'est voulu |
| Identifiants SMTP Brevo | `BREVO_SMTP_LOGIN` / `BREVO_SMTP_KEY` | Dans le tableau de bord Brevo, regenerer la cle SMTP |

Dans tous les cas, la nouvelle valeur va uniquement dans le `.env` local de
chacun ou dans la configuration du serveur - jamais dans un fichier committe.
