package fr.diginamic.hubevenementiel;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Desactive tant qu'aucune base MariaDB n'y est disponible (voir issue #6 / US-001).
// @Disabled sur la methode seule ne suffit pas a empecher SpringExtension de
// tenter de charger le contexte : il faut le desactiver au niveau de la classe.
@Disabled("Necessite une base MariaDB accessible - pas encore provisionnee en CI")
@SpringBootTest
class HubEvenementielApplicationTests {

    @Test
    void contextLoads() {
    }
}
