package cl.casero.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class CaseroWebApplication {

    @Bean
    WebVersionHolder webVersionHolder() {
        WebVersionHolder holder = new WebVersionHolder();
        holder.init();
        return holder;
    }

    @Bean
    WebMvcConfigurer versionInterceptor(WebVersionHolder versionHolder) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new VersionInterceptor(versionHolder));
            }
        };
    }


    public static void main(String[] args) {
        SpringApplication.run(CaseroWebApplication.class, args);
    }
}
