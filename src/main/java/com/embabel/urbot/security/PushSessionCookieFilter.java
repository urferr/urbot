package com.embabel.urbot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Prevents Vaadin push endpoint disconnect handling from creating a
 * spurious HTTP session. When Atmosphere handles a push disconnect on
 * an expired session, it calls request.getSession() (without false),
 * which creates a new empty session whose JSESSIONID cookie overwrites
 * the authenticated one, causing a logout loop.
 *
 * This filter wraps push requests so that getSession() never creates
 * a new session — it only returns an existing one. This stops the
 * problem at the source, before Tomcat can set a cookie.
 *
 * See: https://github.com/vaadin/flow/issues/4072
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class PushSessionCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().contains("/VAADIN/push")) {
            filterChain.doFilter(new NoCreateSessionRequest(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Request wrapper that prevents new session creation on push endpoints.
     * getSession() and getSession(true) return the existing session or null,
     * never creating a new one.
     */
    private static class NoCreateSessionRequest extends HttpServletRequestWrapper {

        NoCreateSessionRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public HttpSession getSession(boolean create) {
            return super.getSession(false);
        }

        @Override
        public HttpSession getSession() {
            return super.getSession(false);
        }
    }
}
