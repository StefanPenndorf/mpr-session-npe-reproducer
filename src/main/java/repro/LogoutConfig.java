package repro;

import jakarta.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Like a Spring Security logout: invalidates the HTTP session without taking the Vaadin lock. */
@Configuration
public class LogoutConfig {

    @Bean
    public FilterRegistrationBean<jakarta.servlet.Filter> logoutFilter() {
        FilterRegistrationBean<jakarta.servlet.Filter> registration = new FilterRegistrationBean<>(
                (request, response, chain) -> {
                    var http = (jakarta.servlet.http.HttpServletRequest) request;
                    HttpSession session = http.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    ((jakarta.servlet.http.HttpServletResponse) response).sendRedirect("/");
                });
        registration.addUrlPatterns("/logout");
        return registration;
    }
}
