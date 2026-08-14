package com.hexagonal.portfolio.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.TransactionType;
import com.hexagonal.portfolio.adapter.out.persistence.repository.amfi.InflationIndexRepository;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.InvestorTransactionCamsRepository;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.TransactionTypeRepository;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.UsersRepository;

/**
 * Tests for the driven adapters.
 *
 * <p>Most of these classes are a repository call plus a mapper call, so the behaviour worth
 * pinning is narrow but real: that entities are translated rather than handed to the application
 * layer, that query arguments arrive unaltered, and that the two adapters carrying actual logic —
 * the paging cap and the transaction-type parsing — do what the ported code did.
 */
@DisplayName("persistence adapters")
class PersistenceAdapterTest {

    @Nested
    @DisplayName("InvestorPersistenceAdapter")
    class Investor {

        private final UsersRepository repository = mock(UsersRepository.class);
        private final InvestorPersistenceAdapter adapter = new InvestorPersistenceAdapter(repository);

        @Test
        @DisplayName("translates the entity into a domain model, never leaking the entity")
        void translatesEntity() {
            com.hexagonal.portfolio.adapter.out.persistence.entity.primary.User entity =
                    new com.hexagonal.portfolio.adapter.out.persistence.entity.primary.User();
            entity.setId(42);
            when(repository.findByIdAndClientName(42, "acme")).thenReturn(Optional.of(entity));

            Optional<com.hexagonal.portfolio.domain.model.User> result =
                    adapter.findByIdAndClientName(42, "acme");

            assertThat(result).isPresent();
            assertThat(result.get())
                    .as("the application layer must never receive a managed entity")
                    .isInstanceOf(com.hexagonal.portfolio.domain.model.User.class)
                    .isNotInstanceOf(
                            com.hexagonal.portfolio.adapter.out.persistence.entity.primary.User.class);
            assertThat(result.get().getId()).isEqualTo(42);
        }

        @Test
        @DisplayName("passes an absent result straight through")
        void emptyStaysEmpty() {
            when(repository.findByIdAndClientName(any(), anyString())).thenReturn(Optional.empty());

            assertThat(adapter.findByIdAndClientName(1, "acme")).isEmpty();
        }

        @Test
        @DisplayName("forwards the lookup arguments unaltered")
        void forwardsArguments() {
            when(repository.findByIdAndClientName(any(), anyString())).thenReturn(Optional.empty());

            adapter.findByIdAndClientName(99, "  spaced  ");

            verify(repository).findByIdAndClientName(99, "  spaced  ");
        }
    }

    @Nested
    @DisplayName("CamsTransactionPersistenceAdapter")
    class CamsTransactions {

        private final InvestorTransactionCamsRepository repository =
                mock(InvestorTransactionCamsRepository.class);
        private final CamsTransactionPersistenceAdapter adapter =
                new CamsTransactionPersistenceAdapter(repository);

        @Test
        @DisplayName("maps every row and preserves order")
        void mapsEveryRow() {
            when(repository.findAllByUserAndDate(any(), anyString(), any()))
                    .thenReturn(List.of(camsEntity(1), camsEntity(2)));

            List<com.hexagonal.portfolio.domain.model.InvestorTransactionCams> result =
                    adapter.findAllByUserAndDate(1, "acme", new Date());

            assertThat(result)
                    .extracting(com.hexagonal.portfolio.domain.model.InvestorTransactionCams::getId)
                    .containsExactly(1, 2);
        }

        /**
         * An empty list rather than null matters downstream: the valuation streams over this
         * result directly, so a null would surface as a NullPointerException mid-calculation.
         */
        @Test
        @DisplayName("returns an empty list, never null, when there are no rows")
        void noRowsYieldsEmptyList() {
            when(repository.findAllByUserAndDate(any(), anyString(), any())).thenReturn(List.of());

            assertThat(adapter.findAllByUserAndDate(1, "acme", new Date())).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("forwards the cut-off date unaltered")
        void forwardsCutoffDate() {
            Date cutoff = new Date(1_700_000_000_000L);
            when(repository.findAllByUserAndDate(any(), anyString(), any())).thenReturn(List.of());

            adapter.findAllByUserAndDate(7, "acme", cutoff);

            verify(repository).findAllByUserAndDate(7, "acme", cutoff);
        }

        private com.hexagonal.portfolio.adapter.out.persistence.entity.primary.InvestorTransactionCams
                camsEntity(int id) {
            var entity =
                    new com.hexagonal.portfolio.adapter.out.persistence.entity.primary.InvestorTransactionCams();
            entity.setId(id);
            return entity;
        }
    }

    @Nested
    @DisplayName("InflationIndexPersistenceAdapter")
    class InflationIndex {

        private final InflationIndexRepository repository = mock(InflationIndexRepository.class);
        private final InflationIndexPersistenceAdapter adapter =
                new InflationIndexPersistenceAdapter(repository);

        /**
         * The legacy service passed {@code PageRequest.of(0, 1)} from inside the application
         * layer. Moving it here is the one deliberate call-site change in the whole port, so the
         * one-row cap is worth pinning explicitly — losing it would silently change which
         * inflation index a capital-gain calculation uses.
         */
        @Test
        @DisplayName("applies the single-row cap the legacy call site used")
        void appliesSingleRowCap() {
            when(repository.findInflationIndex(anyString(), any())).thenReturn(List.of(348.0));

            adapter.findInflationIndex("2024-03-31");

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findInflationIndex(eq("2024-03-31"), pageable.capture());

            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns the index value the query produced")
        void returnsIndexValue() {
            when(repository.findInflationIndex(anyString(), any())).thenReturn(List.of(348.0));

            assertThat(adapter.findInflationIndex("2024-03-31")).containsExactly(348.0);
        }

        @Test
        @DisplayName("returns empty when no index exists for the date")
        void noIndexYieldsEmpty() {
            when(repository.findInflationIndex(anyString(), any())).thenReturn(List.of());

            assertThat(adapter.findInflationIndex("1990-01-01")).isEmpty();
        }
    }

    @Nested
    @DisplayName("TransactionTypeConfigAdapter")
    class TransactionTypeConfig {

        private final TransactionTypeRepository repository = mock(TransactionTypeRepository.class);

        @Test
        @DisplayName("splits each registrar's vocabulary and files it under that registrar")
        void splitsPerRegistrar() {
            when(repository.findAll()).thenReturn(List.of(
                    row("cams", "Purchase,Switch In", "Reinvest"),
                    row("karvy", "Buy,Transfer In", "Bonus"),
                    row("mf_manual", "Manual Buy", "Manual Neutral")));

            TransactionTypeConfigAdapter adapter = new TransactionTypeConfigAdapter(repository);
            adapter.init();

            assertThat(adapter.getCamsPositive()).containsExactly("Purchase", "Switch In");
            assertThat(adapter.getCamsNeutral()).containsExactly("Reinvest");
            assertThat(adapter.getKarvyPositive()).containsExactly("Buy", "Transfer In");
            assertThat(adapter.getKarvyNeutral()).containsExactly("Bonus");
            assertThat(adapter.getMfManualPositive()).containsExactly("Manual Buy");
            assertThat(adapter.getMfManualNeutral()).containsExactly("Manual Neutral");
        }

        @Test
        @DisplayName("matches the registrar name regardless of case")
        void registrarMatchIsCaseInsensitive() {
            when(repository.findAll()).thenReturn(List.of(row("CAMS", "Purchase", "Reinvest")));

            TransactionTypeConfigAdapter adapter = new TransactionTypeConfigAdapter(repository);
            adapter.init();

            assertThat(adapter.getCamsPositive()).containsExactly("Purchase");
        }

        @Test
        @DisplayName("collapses repeated separators rather than producing blank entries")
        void collapsesRepeatedSeparators() {
            when(repository.findAll()).thenReturn(List.of(row("cams", "Purchase,,,Switch In", "Reinvest")));

            TransactionTypeConfigAdapter adapter = new TransactionTypeConfigAdapter(repository);
            adapter.init();

            assertThat(adapter.getCamsPositive()).containsExactly("Purchase", "Switch In");
        }

        @Test
        @DisplayName("leaves a registrar's lists empty when it has no configuration row")
        void unknownRegistrarLeavesListsEmpty() {
            when(repository.findAll()).thenReturn(List.of(row("cams", "Purchase", "Reinvest")));

            TransactionTypeConfigAdapter adapter = new TransactionTypeConfigAdapter(repository);
            adapter.init();

            assertThat(adapter.getKarvyPositive()).isEmpty();
            assertThat(adapter.getMfManualPositive()).isEmpty();
        }

        /**
         * An unrecognised registrar is ignored rather than throwing, so a new row in the
         * configuration table cannot take the service down at startup.
         */
        @Test
        @DisplayName("ignores a registrar it does not recognise")
        void ignoresUnknownRegistrar() {
            when(repository.findAll()).thenReturn(List.of(
                    row("cams", "Purchase", "Reinvest"),
                    row("some_new_registrar", "Whatever", "Nothing")));

            TransactionTypeConfigAdapter adapter = new TransactionTypeConfigAdapter(repository);
            adapter.init();

            assertThat(adapter.getCamsPositive()).containsExactly("Purchase");
        }

        @Test
        @DisplayName("starts with empty vocabularies when the table is empty")
        void emptyTableYieldsEmptyVocabularies() {
            when(repository.findAll()).thenReturn(List.of());

            TransactionTypeConfigAdapter adapter = new TransactionTypeConfigAdapter(repository);
            adapter.init();

            assertThat(adapter.getCamsPositive()).isEmpty();
            assertThat(adapter.getCamsNeutral()).isEmpty();
        }

        private TransactionType row(String registrar, String positive, String neutral) {
            TransactionType type = new TransactionType();
            type.setRegistrar(registrar);
            type.setPositive_transaction(positive);
            type.setNeutral_transaction(neutral);
            return type;
        }
    }
}
