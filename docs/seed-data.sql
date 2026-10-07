-- =====================================================================
-- Jeu de données de test — Hub Événementiel (MariaDB)
-- =====================================================================
-- ATTENTION : la base de dev est PARTAGÉE par toute l'équipe.
--   * Tout ce que ce script crée est repérable : emails en @seed.local,
--     adresses avec address_line2 = 'SEED'. Les clubs/évènements/etc. sont
--     rattachés à ces comptes.
--   * La section 0 supprime d'abord d'anciennes données de seed (le script
--     peut donc être relancé), sans toucher au reste de la base.
--   * La section 7 (documents légaux) est désactivée par défaut : elle
--     changerait la « dernière version » affichée à tout le monde.
--
-- Mot de passe de TOUS les comptes seed : Password12345!
--
-- Table de jointure comptes <-> clubs : app_user_clubs (app_users_id, clubs_id).
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. NETTOYAGE d'un éventuel seed précédent
-- ---------------------------------------------------------------------
DELETE FROM `comment`
 WHERE author_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local')
    OR event_id IN (SELECT id FROM event WHERE organizer_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local'));
DELETE FROM inscription
 WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local')
    OR event_id IN (SELECT id FROM event WHERE organizer_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local'));
DELETE FROM image
 WHERE event_id IN (SELECT id FROM event WHERE organizer_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local'));
DELETE FROM token
 WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local');
DELETE FROM anonymization_demand
 WHERE requester_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local')
    OR administrator_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local');
DELETE FROM legal_document
 WHERE author_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local');
DELETE FROM app_user_clubs
 WHERE app_users_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local')
    OR clubs_id IN (SELECT id FROM club WHERE owner_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local'));
DELETE FROM event
 WHERE organizer_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local');
DELETE FROM club
 WHERE owner_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local');
DELETE FROM app_user WHERE email LIKE '%@seed.local';
DELETE FROM address WHERE address_line2 = 'SEED';

-- ---------------------------------------------------------------------
-- 1. ADRESSES (une par ville)
-- ---------------------------------------------------------------------
INSERT INTO address (address_line1, address_line2, postal_code, city, country)
VALUES ('12 rue des Carmes', 'SEED', '31000', 'Toulouse', 'France');
SET @a_tls = LAST_INSERT_ID();
INSERT INTO address (address_line1, address_line2, postal_code, city, country)
VALUES ('8 place Bellecour', 'SEED', '69002', 'Lyon', 'France');
SET @a_lyo = LAST_INSERT_ID();
INSERT INTO address (address_line1, address_line2, postal_code, city, country)
VALUES ('25 avenue des Champs-Élysées', 'SEED', '75008', 'Paris', 'France');
SET @a_par = LAST_INSERT_ID();
INSERT INTO address (address_line1, address_line2, postal_code, city, country)
VALUES ('3 quai des Chartrons', 'SEED', '33000', 'Bordeaux', 'France');
SET @a_bor = LAST_INSERT_ID();
INSERT INTO address (address_line1, address_line2, postal_code, city, country)
VALUES ('14 rue Crébillon', 'SEED', '44000', 'Nantes', 'France');
SET @a_nan = LAST_INSERT_ID();
INSERT INTO address (address_line1, address_line2, postal_code, city, country)
VALUES ('6 rue Faidherbe', 'SEED', '59000', 'Lille', 'France');
SET @a_lil = LAST_INSERT_ID();

-- ---------------------------------------------------------------------
-- 2. UTILISATEURS (26 : 1 admin, 3 organisateurs, 22 membres)
--    Hash Argon2id (Spring) de « Password12345! ».
-- ---------------------------------------------------------------------
SET @pwd = '$argon2id$v=19$m=16384,t=2,p=1$smdah4eA8l2M0VefbOL5/w$sDC/LDU/hSOoRQ272vSHVN7+u49Kkf4firf9ey/u+p4';

INSERT INTO app_user
  (lastname, firstname, email, hashed_password, phone, role, status, suspension_end_date, creatio_date, address_id)
VALUES
  -- Administrateur et organisateurs
  ('Admin',    'Sophie',  'admin@seed.local',  @pwd, '0600000001', 'ADMINISTRATOR', 'ACTIVE', NULL, CURDATE() - INTERVAL 200 DAY, @a_tls),
  ('Dupont',   'Marc',    'orga1@seed.local',  @pwd, '0600000002', 'ORGANIZER',     'ACTIVE', NULL, CURDATE() - INTERVAL 180 DAY, @a_tls),
  ('Bernard',  'Julie',   'orga2@seed.local',  @pwd, '0600000003', 'ORGANIZER',     'ACTIVE', NULL, CURDATE() - INTERVAL 170 DAY, @a_lyo),
  ('Haddad',   'Karim',   'orga3@seed.local',  @pwd, '0600000004', 'ORGANIZER',     'ACTIVE', NULL, CURDATE() - INTERVAL 160 DAY, @a_par),
  -- Membres actifs
  ('Martin',   'Camille', 'm01@seed.local',    @pwd, '0610000001', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 150 DAY, @a_tls),
  ('Petit',    'Lucas',   'm02@seed.local',    @pwd, '0610000002', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 140 DAY, @a_lyo),
  ('Leroy',    'Emma',    'm03@seed.local',    @pwd, '0610000003', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 130 DAY, @a_par),
  ('Moreau',   'Hugo',    'm04@seed.local',    @pwd, '0610000004', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 120 DAY, @a_bor),
  ('Simon',    'Chloé',   'm05@seed.local',    @pwd, '0610000005', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 110 DAY, @a_nan),
  ('Laurent',  'Louis',   'm06@seed.local',    @pwd, '0610000006', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 100 DAY, @a_lil),
  ('Michel',   'Léa',     'm07@seed.local',    @pwd, '0610000007', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 95 DAY,  @a_tls),
  ('Garcia',   'Nathan',  'm08@seed.local',    @pwd, '0610000008', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 90 DAY,  @a_lyo),
  ('David',    'Manon',   'm09@seed.local',    @pwd, '0610000009', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 85 DAY,  @a_par),
  -- Membres sans aucune activité (suppression directe possible)
  ('Bertrand', 'Théo',    'm10@seed.local',    @pwd, '0610000010', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 80 DAY,  @a_bor),
  -- m11 : seulement un commentaire (voir section 6) -> test ACCOUNT_HAS_COMMENTS
  ('Roux',     'Inès',    'm11@seed.local',    @pwd, '0610000011', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 75 DAY,  NULL),
  ('Vincent',  'Jules',   'm12@seed.local',    @pwd, '0610000012', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 70 DAY,  @a_nan),
  ('Fournier', 'Sarah',   'm13@seed.local',    @pwd, '0610000013', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 65 DAY,  NULL),
  ('Morel',    'Maxime',  'm14@seed.local',    @pwd, '0610000014', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 60 DAY,  @a_lil),
  ('Girard',   'Zoé',     'm15@seed.local',    @pwd, '0610000015', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 55 DAY,  NULL),
  ('André',    'Antoine', 'm16@seed.local',    @pwd, '0610000016', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 50 DAY,  @a_tls),
  -- Autres statuts
  ('Lefebvre', 'Alice',   'm17@seed.local',    @pwd, '0610000017', 'MEMBER', 'SUSPENDED',          NOW() + INTERVAL 14 DAY, CURDATE() - INTERVAL 45 DAY, @a_lyo),
  ('Mercier',  'Paul',    'm18@seed.local',    @pwd, '0610000018', 'MEMBER', 'PENDING_ACTIVATION', NULL, CURDATE() - INTERVAL 1 DAY,  NULL),
  ('Dupuis',   'Clara',   'm19@seed.local',    @pwd, '0610000019', 'MEMBER', 'INACTIVE',           NULL, CURDATE() - INTERVAL 40 DAY, @a_par),
  ('ANONYME-0001', 'ANONYME-0001', 'anonyme-0001@seed.local', @pwd, 'ANONYME-0001', 'MEMBER', 'ANONYMIZE', NULL, CURDATE() - INTERVAL 35 DAY, NULL),
  ('Blanc',    'Romain',  'm21@seed.local',    @pwd, '0610000021', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 30 DAY,  @a_bor),
  ('Guérin',   'Eva',     'm22@seed.local',    @pwd, '0610000022', 'MEMBER', 'ACTIVE', NULL, CURDATE() - INTERVAL 20 DAY,  @a_nan);

SELECT id INTO @admin FROM app_user WHERE email = 'admin@seed.local';
SELECT id INTO @orga1 FROM app_user WHERE email = 'orga1@seed.local';
SELECT id INTO @orga2 FROM app_user WHERE email = 'orga2@seed.local';
SELECT id INTO @orga3 FROM app_user WHERE email = 'orga3@seed.local';

-- Token d'activation pour m18 (mot de passe temporaire : Password12345!)
-- Lien : <frontend>/activate-account?token=seed-activation-token-0001
INSERT INTO token (`value`, creation_date_time, expiration_date_time, pending_data, `type`, user_id)
SELECT 'seed-activation-token-0001', NOW(), NOW() + INTERVAL 7 DAY, '', 'ACCOUNT_ACTIVATION', id
  FROM app_user WHERE email = 'm18@seed.local';

-- ---------------------------------------------------------------------
-- 3. CLUBS + affiliations
-- ---------------------------------------------------------------------
INSERT INTO club (name, category, email, phone, end_validity_date, owner_id, address_id)
VALUES ('Toulouse Escalade Club', 'SPORT', 'contact@toulouse-escalade.seed', '0561000001', NULL, @orga1, @a_tls);
SET @c1 = LAST_INSERT_ID();
INSERT INTO club (name, category, email, phone, end_validity_date, owner_id, address_id)
VALUES ('Orchestre de Lyon', 'CULTURE', 'contact@orchestre-lyon.seed', '0472000002', NULL, @orga2, @a_lyo);
SET @c2 = LAST_INSERT_ID();
INSERT INTO club (name, category, email, phone, end_validity_date, owner_id, address_id)
VALUES ('Les Joueurs de Paris', 'DIVERTISSEMENT', 'contact@joueurs-paris.seed', '0140000003', NULL, @orga3, @a_par);
SET @c3 = LAST_INSERT_ID();
INSERT INTO club (name, category, email, phone, end_validity_date, owner_id, address_id)
VALUES ('Bordeaux Running', 'SPORT', 'contact@bordeaux-running.seed', '0556000004', NULL, @orga1, @a_bor);
SET @c4 = LAST_INSERT_ID();
INSERT INTO club (name, category, email, phone, end_validity_date, owner_id, address_id)
VALUES ('Atelier Photo Nantes', 'DIVERTISSEMENT', 'contact@photo-nantes.seed', '0240000005', CURDATE() - INTERVAL 30 DAY, @orga2, @a_nan);
SET @c5 = LAST_INSERT_ID();
INSERT INTO club (name, category, email, phone, end_validity_date, owner_id, address_id)
VALUES ('Ciné-club de Lille', 'CULTURE', 'contact@cineclub-lille.seed', '0320000006', NULL, @orga3, @a_lil);
SET @c6 = LAST_INSERT_ID();

-- Table de jointure comptes <-> clubs
INSERT INTO app_user_clubs (app_users_id, clubs_id)
SELECT id, @c1 FROM app_user WHERE email IN ('m01@seed.local', 'm07@seed.local', 'm16@seed.local');
INSERT INTO app_user_clubs (app_users_id, clubs_id)
SELECT id, @c2 FROM app_user WHERE email IN ('m02@seed.local', 'm08@seed.local');
INSERT INTO app_user_clubs (app_users_id, clubs_id)
SELECT id, @c3 FROM app_user WHERE email IN ('m03@seed.local', 'm09@seed.local', 'm19@seed.local');
INSERT INTO app_user_clubs (app_users_id, clubs_id)
SELECT id, @c4 FROM app_user WHERE email IN ('m04@seed.local', 'm21@seed.local');
INSERT INTO app_user_clubs (app_users_id, clubs_id)
SELECT id, @c6 FROM app_user WHERE email IN ('m06@seed.local', 'm14@seed.local');
-- Chaque organisateur est affilié aux clubs dont il est propriétaire.
INSERT INTO app_user_clubs (app_users_id, clubs_id)
SELECT owner_id, id FROM club WHERE id IN (@c1, @c2, @c3, @c4, @c5, @c6);
-- Les membres m10 à m13, m15, m22 n'ont aucun club.

-- ---------------------------------------------------------------------
-- 4. ÉVÈNEMENTS (12)
-- ---------------------------------------------------------------------
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Initiation à l''escalade', 'Une demi-journée pour découvrir l''escalade en salle, matériel fourni.',
        @a_tls, 'SPORT', NOW() + INTERVAL 10 DAY, NOW() + INTERVAL 10 DAY + INTERVAL 3 HOUR, 10.00, 15.00, 12, 12, 'PUBLISHED', @orga1);
SET @e1 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Concert de printemps', 'Le grand concert annuel de l''orchestre : symphonies de Beethoven et Brahms.',
        @a_lyo, 'CULTURE', NOW() + INTERVAL 20 DAY, NOW() + INTERVAL 20 DAY + INTERVAL 2 HOUR, 15.00, 25.00, 200, 200, 'PUBLISHED', @orga2);
SET @e2 = LAST_INSERT_ID();
-- Capacité de 3 seulement : permet de tester la liste d'attente
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Tournoi de jeux de société', 'Après-midi jeux de plateau, de la stratégie à l''ambiance.',
        @a_par, 'DIVERTISSEMENT', NOW() + INTERVAL 5 DAY, NOW() + INTERVAL 5 DAY + INTERVAL 4 HOUR, 5.00, 8.00, 3, 3, 'PUBLISHED', @orga3);
SET @e3 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Semi-marathon des quais', 'Parcours de 21 km le long de la Garonne, ravitaillements prévus.',
        @a_bor, 'SPORT', NOW() + INTERVAL 45 DAY, NOW() + INTERVAL 45 DAY + INTERVAL 4 HOUR, 20.00, 30.00, 500, 500, 'PUBLISHED', @orga1);
SET @e4 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Exposition photo (brouillon)', 'Vernissage de l''exposition collective du club photo.',
        @a_nan, 'DIVERTISSEMENT', NOW() + INTERVAL 30 DAY, NOW() + INTERVAL 30 DAY + INTERVAL 5 HOUR, 0.00, 5.00, 60, 60, 'DRAFT', @orga2);
SET @e5 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Ciné-débat : le cinéma muet', 'Projection suivie d''un débat avec un historien du cinéma.',
        @a_lil, 'CULTURE', NOW() + INTERVAL 3 DAY, NOW() + INTERVAL 3 DAY + INTERVAL 3 HOUR, 6.00, 9.00, 80, 80, 'PUBLISHED', @orga3);
SET @e6 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Soirée jazz', 'Une soirée jazz dans une cave voûtée, trois formations.',
        @a_lyo, 'CULTURE', NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY + INTERVAL 3 HOUR, 12.00, 18.00, 90, 90, 'FINISHED', @orga2);
SET @e7 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Randonnée des crêtes', 'Journée de randonnée, niveau intermédiaire, pique-nique partagé.',
        @a_tls, 'SPORT', NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY + INTERVAL 8 HOUR, 0.00, 5.00, 25, 25, 'FINISHED', @orga1);
SET @e8 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Atelier peinture (annulé)', 'Atelier d''aquarelle annulé faute d''animateur.',
        @a_nan, 'DIVERTISSEMENT', NOW() + INTERVAL 12 DAY, NOW() + INTERVAL 12 DAY + INTERVAL 3 HOUR, 8.00, 12.00, 15, 15, 'CANCELLED', @orga2);
SET @e9 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Tournoi de tennis', 'Tournoi en simple, tous niveaux, tableaux par catégories.',
        @a_tls, 'SPORT', NOW() + INTERVAL 60 DAY, NOW() + INTERVAL 61 DAY, 15.00, 22.00, 64, 64, 'PUBLISHED', @orga1);
SET @e10 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Festival de théâtre', 'Trois jours de théâtre contemporain et de compagnies émergentes.',
        @a_par, 'CULTURE', NOW() + INTERVAL 90 DAY, NOW() + INTERVAL 92 DAY, 25.00, 35.00, 300, 300, 'PUBLISHED', @orga2);
SET @e11 = LAST_INSERT_ID();
INSERT INTO event (title, description, location_id, category, start_date_time, end_date_time,
                   affiliate_price, non_affiliate_price, max_capacity, remaining_places, status, organizer_id)
VALUES ('Rencontre échecs', 'Parties libres et mini-tournoi, échiquiers fournis.',
        @a_par, 'DIVERTISSEMENT', NOW() + INTERVAL 7 DAY, NOW() + INTERVAL 7 DAY + INTERVAL 3 HOUR, 3.00, 5.00, 30, 30, 'PUBLISHED', @orga3);
SET @e12 = LAST_INSERT_ID();

-- ---------------------------------------------------------------------
-- 5. INSCRIPTIONS
-- ---------------------------------------------------------------------
-- Tournoi de jeux (@e3, capacité 3) : 3 confirmés + 2 en liste d'attente.
-- Supprimer un confirmé sans commentaire (m01 ou m02) doit promouvoir m04 depuis la liste d'attente.
-- (m03 a posté un commentaire : sa suppression est d'abord refusée.)
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e3, NOW() - INTERVAL 4 DAY, 'CONFIRMED', 5.00 FROM app_user WHERE email IN ('m01@seed.local', 'm02@seed.local', 'm03@seed.local');
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e3, NOW() - INTERVAL 3 DAY, 'WAITING_LIST', 8.00 FROM app_user WHERE email = 'm04@seed.local';
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e3, NOW() - INTERVAL 2 DAY, 'WAITING_LIST', 8.00 FROM app_user WHERE email = 'm05@seed.local';

-- Escalade (@e1) : confirmés + une annulation par le membre
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e1, NOW() - INTERVAL 6 DAY, 'CONFIRMED', 10.00 FROM app_user WHERE email IN ('m01@seed.local', 'm07@seed.local', 'm16@seed.local');
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e1, NOW() - INTERVAL 5 DAY, 'CONFIRMED', 15.00 FROM app_user WHERE email IN ('m05@seed.local', 'm08@seed.local');
INSERT INTO inscription (user_id, event_id, inscription_date, status, price, cancellation_date, canceled_by_id)
SELECT id, @e1, NOW() - INTERVAL 5 DAY, 'CANCELED', 15.00, NOW() - INTERVAL 1 DAY, id FROM app_user WHERE email = 'm09@seed.local';

-- Concert (@e2), semi-marathon (@e4), ciné-débat (@e6), échecs (@e12)
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e2, NOW() - INTERVAL 8 DAY, 'CONFIRMED', 15.00 FROM app_user WHERE email IN ('m02@seed.local', 'm03@seed.local', 'm06@seed.local', 'm08@seed.local');
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e4, NOW() - INTERVAL 7 DAY, 'CONFIRMED', 20.00 FROM app_user WHERE email IN ('m04@seed.local', 'm21@seed.local', 'm01@seed.local');
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e6, NOW() - INTERVAL 2 DAY, 'CONFIRMED', 6.00 FROM app_user WHERE email IN ('m06@seed.local', 'm07@seed.local');
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e12, NOW() - INTERVAL 3 DAY, 'CONFIRMED', 3.00 FROM app_user WHERE email IN ('m03@seed.local', 'm09@seed.local', 'm05@seed.local');

-- Évènements terminés (@e7 jazz, @e8 randonnée) : historique
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e7, NOW() - INTERVAL 25 DAY, 'CONFIRMED', 12.00 FROM app_user WHERE email IN ('m02@seed.local', 'm05@seed.local', 'm08@seed.local');
INSERT INTO inscription (user_id, event_id, inscription_date, status, price)
SELECT id, @e8, NOW() - INTERVAL 40 DAY, 'CONFIRMED', 0.00 FROM app_user WHERE email IN ('m01@seed.local', 'm07@seed.local', 'm16@seed.local');

-- Colonne héritée remaining_places (présente en base, plus dans l'entité) :
-- on la recale sur les inscriptions confirmées.
UPDATE event e
   SET e.remaining_places = e.max_capacity
       - (SELECT COUNT(*) FROM inscription i WHERE i.event_id = e.id AND i.status = 'CONFIRMED')
 WHERE e.organizer_id IN (SELECT id FROM app_user WHERE email LIKE '%@seed.local');

-- ---------------------------------------------------------------------
-- 6. COMMENTAIRES
-- ---------------------------------------------------------------------
INSERT INTO `comment` (author_id, event_id, content, creation_date)
SELECT id, @e7, 'Super soirée, l''ambiance de la cave était parfaite !', NOW() - INTERVAL 14 DAY FROM app_user WHERE email = 'm08@seed.local';
INSERT INTO `comment` (author_id, event_id, content, creation_date)
SELECT id, @e7, 'Un peu serré mais les musiciens étaient excellents.', NOW() - INTERVAL 13 DAY FROM app_user WHERE email = 'm05@seed.local';
INSERT INTO `comment` (author_id, event_id, content, creation_date)
SELECT id, @e8, 'Belle rando, merci à l''organisateur pour le parcours.', NOW() - INTERVAL 29 DAY FROM app_user WHERE email = 'm16@seed.local';
INSERT INTO `comment` (author_id, event_id, content, creation_date)
SELECT id, @e1, 'Y a-t-il une limite d''âge pour l''initiation ?', NOW() - INTERVAL 2 DAY FROM app_user WHERE email = 'm03@seed.local';
-- m11 : SEUL son commentaire le lie à une activité (n'est inscrit nulle part)
INSERT INTO `comment` (author_id, event_id, content, creation_date)
SELECT id, @e2, 'Hâte d''y être ! Le programme est très alléchant.', NOW() - INTERVAL 1 DAY FROM app_user WHERE email = 'm11@seed.local';

-- ---------------------------------------------------------------------
-- 6 bis. DEMANDES D'ANONYMISATION
-- ---------------------------------------------------------------------
INSERT INTO anonymization_demand (request_status, demand_date, approved_date, requester_id, administrator_id)
SELECT 'PENDING', NOW() - INTERVAL 2 DAY, NULL, id, NULL FROM app_user WHERE email = 'm06@seed.local';
INSERT INTO anonymization_demand (request_status, demand_date, approved_date, requester_id, administrator_id)
SELECT 'PENDING', NOW() - INTERVAL 1 DAY, NULL, id, NULL FROM app_user WHERE email = 'm13@seed.local';
INSERT INTO anonymization_demand (request_status, demand_date, approved_date, requester_id, administrator_id)
SELECT 'VALIDATE', NOW() - INTERVAL 36 DAY, NOW() - INTERVAL 35 DAY, id, @admin FROM app_user WHERE email = 'anonyme-0001@seed.local';

-- ---------------------------------------------------------------------
-- 7. DOCUMENTS LÉGAUX (DÉSACTIVÉ : décommente si tu veux les créer)
--    Attention : base partagée, ces versions pourraient devenir les
--    « dernières versions » vues par toute l'équipe.
-- ---------------------------------------------------------------------
-- INSERT INTO legal_document (type, content, version, update_date, pdf_path, author_id) VALUES
--   ('TERM_OF_USE', 'Conditions d''utilisation (seed) — version 1.', 1, NOW() - INTERVAL 60 DAY, NULL, @admin),
--   ('TERM_OF_USE', 'Conditions d''utilisation (seed) — version 2.', 2, NOW() - INTERVAL 10 DAY, NULL, @admin),
--   ('GDPR_POLICY', 'Politique RGPD (seed) — version 1.',            1, NOW() - INTERVAL 60 DAY, NULL, @admin);

-- ---------------------------------------------------------------------
-- Comptes utiles pour les tests (mot de passe Password12345!) :
--   admin@seed.local          administrateur
--   orga1@seed.local          organisateur : évènements + clubs  -> suppression refusée (409)
--   m10@seed.local            aucune activité                    -> suppression directe
--   m11@seed.local            un commentaire seulement           -> X-Error-Code: ACCOUNT_HAS_COMMENTS
--   m01@seed.local            confirmé au tournoi (@e3)          -> sa suppression promeut m04
--   m17@seed.local            suspendu (jusqu'à J+14)
--   m18@seed.local            en attente d'activation (token seed-activation-token-0001)
--   m19@seed.local            inactif
--   anonyme-0001@seed.local   anonymisé
-- ---------------------------------------------------------------------
