package com.global.ct.frameinventory.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FrameTimeConfiguration {

    @Bean
    Clock frameClock() {
        return Clock.systemUTC();
    }
}
