package io.github.xfoundries.demo.payment.boot.config;

import java.time.Clock;
import java.util.UUID;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import io.github.xfoundries.demo.payment.application.ExpenseClaimApprovedProcessor;
import io.github.xfoundries.demo.payment.application.PaymentRule;
import org.jfoundry.application.inbox.InboxTemplate;
import org.jfoundry.application.outbox.OutboxTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentLimitProperties.class)
public class PaymentSimulatorConfiguration {

    @Bean
    PaymentRule paymentRule(PaymentLimitProperties properties) {
        return new PaymentRule(properties.amount(), properties.currency());
    }

    @Bean
    Clock paymentClock() {
        return Clock.systemUTC();
    }

    @Bean
    ExpenseClaimApprovedProcessor expenseClaimApprovedProcessor(
            InboxTemplate inboxTemplate,
            OutboxTemplate outboxTemplate,
            PaymentRule paymentRule,
            Clock paymentClock) {
        return new ExpenseClaimApprovedProcessor(
                inboxTemplate,
                outboxTemplate,
                paymentRule,
                paymentClock,
                () -> UUID.randomUUID().toString());
    }

    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
