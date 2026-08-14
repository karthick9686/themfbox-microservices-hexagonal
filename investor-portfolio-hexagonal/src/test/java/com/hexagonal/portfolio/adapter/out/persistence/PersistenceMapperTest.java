package com.hexagonal.portfolio.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.InvestorTransactionCams;

/**
 * Tests for the reflective entity → domain mapper.
 *
 * <p>Worth testing carefully despite its size. Every row this service reads passes through here,
 * and because the copy is reflective it fails *silently*: a field present on the entity but not on
 * the domain model is simply not copied, and nothing reports it. That is the failure mode the
 * standards review singled out, and the parity test below is what would catch it.
 */
@DisplayName("PersistenceMapper")
class PersistenceMapperTest {

    @Nested
    @DisplayName("map")
    class Map {

        @Test
        @DisplayName("copies matching properties across")
        void copiesProperties() {
            InvestorTransactionCams entity = new InvestorTransactionCams();
            entity.setId(7);
            entity.setUnits(123.45);
            entity.setTrxn_type_("Purchase");
            entity.setTraddate(new Date(1_600_000_000_000L));

            com.hexagonal.portfolio.domain.model.InvestorTransactionCams domain =
                    PersistenceMapper.map(entity, com.hexagonal.portfolio.domain.model.InvestorTransactionCams.class);

            assertThat(domain.getId()).isEqualTo(7);
            assertThat(domain.getUnits()).isEqualTo(123.45);
            assertThat(domain.getTrxn_type_()).isEqualTo("Purchase");
            assertThat(domain.getTraddate()).isEqualTo(new Date(1_600_000_000_000L));
        }

        @Test
        @DisplayName("returns null for a null source rather than an empty object")
        void nullSourceYieldsNull() {
            assertThat(PersistenceMapper.map(null, com.hexagonal.portfolio.domain.model.InvestorTransactionCams.class))
                    .isNull();
        }

        @Test
        @DisplayName("leaves unset entity fields null rather than defaulting them")
        void unsetFieldsStayNull() {
            com.hexagonal.portfolio.domain.model.InvestorTransactionCams domain =
                    PersistenceMapper.map(new InvestorTransactionCams(),
                            com.hexagonal.portfolio.domain.model.InvestorTransactionCams.class);

            assertThat(domain).isNotNull();
            assertThat(domain.getUnits()).isNull();
            assertThat(domain.getTrxn_type_()).isNull();
        }

        /**
         * The mapper needs a no-argument constructor to instantiate the target. A domain model
         * without one fails at runtime, on the first row read, not at startup.
         */
        @Test
        @DisplayName("fails loudly when the target has no no-arg constructor")
        void noDefaultConstructorFailsLoudly() {
            assertThatThrownBy(() -> PersistenceMapper.map(new InvestorTransactionCams(), NoDefaultConstructor.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot instantiate")
                    .hasCauseInstanceOf(ReflectiveOperationException.class);
        }
    }

    @Nested
    @DisplayName("mapList")
    class MapList {

        @Test
        @DisplayName("maps every element and preserves order")
        void mapsEveryElement() {
            List<InvestorTransactionCams> entities = new ArrayList<>();
            entities.add(entity(1));
            entities.add(entity(2));
            entities.add(entity(3));

            List<com.hexagonal.portfolio.domain.model.InvestorTransactionCams> mapped =
                    PersistenceMapper.mapList(entities,
                            com.hexagonal.portfolio.domain.model.InvestorTransactionCams.class);

            assertThat(mapped)
                    .extracting(com.hexagonal.portfolio.domain.model.InvestorTransactionCams::getId)
                    .containsExactly(1, 2, 3);
        }

        /**
         * Returning an empty list rather than null is load-bearing: the valuation streams over
         * these results directly, so a null would become a NullPointerException deep inside the
         * calculation rather than an empty portfolio.
         */
        @Test
        @DisplayName("returns an empty list for a null source, never null")
        void nullSourceYieldsEmptyList() {
            assertThat(PersistenceMapper.mapList(null,
                    com.hexagonal.portfolio.domain.model.InvestorTransactionCams.class))
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        @DisplayName("returns an empty list for an empty source")
        void emptySourceYieldsEmptyList() {
            assertThat(PersistenceMapper.mapList(List.of(),
                    com.hexagonal.portfolio.domain.model.InvestorTransactionCams.class))
                    .isEmpty();
        }

        @Test
        @DisplayName("maps a null element to null rather than skipping it")
        void nullElementBecomesNull() {
            List<InvestorTransactionCams> withNull = new ArrayList<>();
            withNull.add(entity(1));
            withNull.add(null);

            assertThat(PersistenceMapper.mapList(withNull,
                    com.hexagonal.portfolio.domain.model.InvestorTransactionCams.class))
                    .hasSize(2)
                    .containsNull();
        }
    }

    @Nested
    @DisplayName("entity / domain parity")
    class Parity {

        /**
         * The mapper copies by property name, so a field that exists on the entity but is missing
         * or renamed on the domain model is dropped in silence — the read succeeds and the value
         * is simply absent from the response.
         *
         * <p>This compares the two side by side and fails with the offending names, which is the
         * only cheap way to notice. If it ever fails, either add the field to the domain model or
         * map it explicitly; do not delete the assertion.
         */
        @Test
        @DisplayName("every entity property has a domain counterpart")
        void everyEntityPropertyIsCopied() {
            assertNoDroppedProperties(
                    InvestorTransactionCams.class,
                    com.hexagonal.portfolio.domain.model.InvestorTransactionCams.class);
            assertNoDroppedProperties(
                    com.hexagonal.portfolio.adapter.out.persistence.entity.primary.User.class,
                    com.hexagonal.portfolio.domain.model.User.class);
            assertNoDroppedProperties(
                    com.hexagonal.portfolio.adapter.out.persistence.entity.primary.InvestorMasterCams.class,
                    com.hexagonal.portfolio.domain.model.InvestorMasterCams.class);
            assertNoDroppedProperties(
                    com.hexagonal.portfolio.adapter.out.persistence.entity.primary.UsersMapping.class,
                    com.hexagonal.portfolio.domain.model.UsersMapping.class);
        }

        private void assertNoDroppedProperties(Class<?> entityType, Class<?> domainType) {
            List<String> domainSetters = new ArrayList<>();
            for (var method : domainType.getMethods()) {
                if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                    domainSetters.add(method.getName());
                }
            }

            List<String> dropped = new ArrayList<>();
            for (var method : entityType.getMethods()) {
                boolean isGetter = (method.getName().startsWith("get") || method.getName().startsWith("is"))
                        && method.getParameterCount() == 0
                        && !"getClass".equals(method.getName());
                if (!isGetter) {
                    continue;
                }
                String property = method.getName().startsWith("get")
                        ? method.getName().substring(3)
                        : method.getName().substring(2);
                if (!domainSetters.contains("set" + property)) {
                    dropped.add(property);
                }
            }

            assertThat(dropped)
                    .as("%s properties with no setter on %s — these are silently dropped by the "
                            + "reflective copy", entityType.getSimpleName(), domainType.getSimpleName())
                    .isEmpty();
        }
    }

    // ---------------------------------------------------------------------

    private static InvestorTransactionCams entity(int id) {
        InvestorTransactionCams entity = new InvestorTransactionCams();
        entity.setId(id);
        return entity;
    }

    /** Deliberately has no no-argument constructor. */
    static class NoDefaultConstructor {
        @SuppressWarnings("unused")
        NoDefaultConstructor(String required) {
        }
    }
}
