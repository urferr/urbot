package com.embabel.urbot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

/**
 * Temporary diagnostic filter to debug session cookie issues.
 * Runs BEFORE the Spring Security filter chain.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SessionDiagnosticFilter extends OncePerRequestFilter implements HttpSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionDiagnosticFilter.class);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        logger.warn("SESSION CREATED: {} | thread={}",
                abbrev(se.getSession().getId()),
                Thread.currentThread().getName());
        // Log stack trace to see WHO is creating the session
        logger.warn("SESSION CREATED stack:", new Throwable("Session creation trace"));
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        logger.warn("SESSION DESTROYED: {} | thread={}",
                abbrev(se.getSession().getId()),
                Thread.currentThread().getName());
        logger.warn("SESSION DESTROYED stack:", new Throwable("Session destruction trace"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.contains("/VAADIN/build/") && !uri.contains(".js") && !uri.contains(".css")) {
            String jsessionid = findJSessionId(request);
            HttpSession session = request.getSession(false);
            String serverId = session != null ? session.getId() : "NO_SESSION";
            boolean cookieMismatch = jsessionid != null && session != null
                    && !session.getId().equals(jsessionid);

            logger.warn("SESSION DIAG: {} {} | cookie={} | server={} | mismatch={}",
                    request.getMethod(), uri,
                    abbrev(jsessionid), abbrev(serverId), cookieMismatch);
        }

        filterChain.doFilter(request, response);

        // Check if the response is setting a new JSESSIONID cookie
        if (!uri.contains("/VAADIN/build/") && !uri.contains(".js") && !uri.contains(".css")) {
            Collection<String> setCookieHeaders = response.getHeaders("Set-Cookie");
            for (String header : setCookieHeaders) {
                if (header.contains("JSESSIONID")) {
                    logger.warn("SET-COOKIE in response: {} {} | {}",
                            request.getMethod(), uri, header);
                }
            }
        }
    }

    private String findJSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("JSESSIONID".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private String abbrev(String id) {
        if (id == null) return "NONE";
        return id.substring(0, Math.min(8, id.length())) + "...";
    }
}
