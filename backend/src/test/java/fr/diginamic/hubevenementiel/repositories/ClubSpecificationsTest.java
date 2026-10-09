package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.enums.Category;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class ClubSpecificationsTest {

    @Mock
    private Root<Club> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path<Object> namePath;
    @Mock
    private Path<Object> addressPath;
    @Mock
    private Path<Object> cityPath;
    @Mock
    private Path<Object> categoryPath;
    @Mock
    private Expression<String> lowered;
    @Mock
    private Predicate like;
    @Mock
    private Predicate equal;
    @Mock
    private Predicate or;

    @BeforeEach
    void setUp() {
        // Les stubs ne sont utilisés que par certains tests : lenient évite les faux positifs de Mockito strict.
        org.mockito.Mockito.lenient().when(root.get("name")).thenReturn(namePath);
        org.mockito.Mockito.lenient().when(root.get("address")).thenReturn(addressPath);
        org.mockito.Mockito.lenient().when(addressPath.get("city")).thenReturn(cityPath);
        org.mockito.Mockito.lenient().when(root.get("category")).thenReturn(categoryPath);
    }

    // ---------------------------------------------------------------
    // matchNames
    // ---------------------------------------------------------------

    @Test
    void matchNames_nullOrBlank_returnsNullSpecification() {
        assertThat(ClubSpecifications.matchNames(null)).isNull();
        assertThat(ClubSpecifications.matchNames("")).isNull();
        assertThat(ClubSpecifications.matchNames("   ")).isNull();
    }

    @Test
    void matchNames_filtersOnLowerCasedTrimmedNameWithWildcards() {
        when(cb.lower((Expression) namePath)).thenReturn(lowered);
        when(cb.like(lowered, "%tennis%")).thenReturn(like);
        when(cb.or(like)).thenReturn(or);

        Specification<Club> spec = ClubSpecifications.matchNames("  TeNNis ");

        assertThat(spec).isNotNull();
        assertThat(spec.toPredicate(root, query, cb)).isSameAs(or);
        verify(cb).like(lowered, "%tennis%");
    }

    // ---------------------------------------------------------------
    // hasCategory
    // ---------------------------------------------------------------

    @Test
    void hasCategory_null_returnsNullSpecification() {
        assertThat(ClubSpecifications.hasCategory(null)).isNull();
    }

    @Test
    void hasCategory_filtersOnExactCategory() {
        Category category = Category.values()[0];
        when(cb.equal(categoryPath, category)).thenReturn(equal);

        Specification<Club> spec = ClubSpecifications.hasCategory(category);

        assertThat(spec).isNotNull();
        assertThat(spec.toPredicate(root, query, cb)).isSameAs(equal);
    }

    // ---------------------------------------------------------------
    // inCity
    // ---------------------------------------------------------------

    @Test
    void inCity_nullOrBlank_returnsNullSpecification() {
        assertThat(ClubSpecifications.inCity(null)).isNull();
        assertThat(ClubSpecifications.inCity(" ")).isNull();
    }

    @Test
    void inCity_navigatesThroughAddressToTheCityColumn() {
        when(cb.lower((Expression) cityPath)).thenReturn(lowered);
        when(cb.like(lowered, "%montpellier%")).thenReturn(like);
        when(cb.or(like)).thenReturn(or);

        Specification<Club> spec = ClubSpecifications.inCity(" Montpellier");

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(or);
        // Régression : la ville n'est pas une colonne de Club, il faut passer par "address".
        verify(root).get("address");
        verify(addressPath).get("city");
        verify(root, never()).get("city");
    }
}
