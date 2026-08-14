package com.hexagonal.portfolio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import com.hexagonal.portfolio.application.port.in.GetInvestorTaxReportUseCase;
import com.hexagonal.portfolio.application.port.out.LoadCamsTaxTransactionPort;
import com.hexagonal.portfolio.application.port.out.LoadInflationIndexPort;
import com.hexagonal.portfolio.application.port.out.LoadKarvyTaxTransactionPort;
import com.hexagonal.portfolio.application.port.out.LoadNavHistoryPort;
import com.hexagonal.portfolio.application.port.out.LoadSchemeMasterPort;
import com.hexagonal.portfolio.application.port.out.LoadTransactionTypePort;

/**
 * Guards the one runtime hazard in making the application services package-private.
 *
 * <p>{@code InvestorTaxReportService} is annotated {@code @Transactional}, so Spring must wrap it in
 * a proxy for that annotation to mean anything. Spring Boot proxies by subclassing (CGLIB), and
 * subclassing a package-private class only works when the generated proxy lands in the same package.
 * Reducing visibility is therefore not a purely cosmetic change, and nothing else in the suite would
 * notice if it broke — the failure would surface at context startup, or worse, as a silently
 * non-transactional service.
 *
 * <p>This drives the same {@link ProxyFactory} path Spring uses and asserts the proxy is created,
 * is a CGLIB subclass, and still satisfies its inbound port.
 */
@DisplayName("package-private application services stay proxyable")
class ApplicationServiceProxyTest {

    @Test
    @DisplayName("the @Transactional tax service can still be CGLIB-proxied")
    void transactionalServiceCanBeProxied() {
        InvestorTaxReportService service = new InvestorTaxReportService(
                mock(LoadTransactionTypePort.class),
                mock(LoadCamsTaxTransactionPort.class),
                mock(LoadKarvyTaxTransactionPort.class),
                mock(LoadSchemeMasterPort.class),
                mock(LoadNavHistoryPort.class),
                mock(LoadInflationIndexPort.class));

        ProxyFactory factory = new ProxyFactory(service);
        factory.setProxyTargetClass(true);

        Object proxy = factory.getProxy(getClass().getClassLoader());

        assertThat(AopUtils.isCglibProxy(proxy))
                .as("Spring Boot proxies by subclassing; a package-private class must still be subclassable")
                .isTrue();
        assertThat(proxy).isInstanceOf(GetInvestorTaxReportUseCase.class);
        assertThat(proxy).isInstanceOf(InvestorTaxReportService.class);
    }
}
