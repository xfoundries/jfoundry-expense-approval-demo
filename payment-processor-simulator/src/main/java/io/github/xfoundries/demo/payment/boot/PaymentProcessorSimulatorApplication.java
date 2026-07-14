package io.github.xfoundries.demo.payment.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.github.xfoundries.demo.payment")
public class PaymentProcessorSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessorSimulatorApplication.class, args);
    }
}
