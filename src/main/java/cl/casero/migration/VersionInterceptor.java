package cl.casero.migration;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class VersionInterceptor implements HandlerInterceptor {

    private final WebVersionHolder holder;

    public VersionInterceptor(WebVersionHolder holder) {
        this.holder = holder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("version", holder.getVersion());
        return true;
    }
}
