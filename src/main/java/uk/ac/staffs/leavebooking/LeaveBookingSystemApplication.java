package uk.ac.staffs.leavebooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRabbit
@EnableAsync
@EnableRetry
@EnableScheduling
@SpringBootApplication
public class LeaveBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveBookingSystemApplication.class, args);
    }

}
