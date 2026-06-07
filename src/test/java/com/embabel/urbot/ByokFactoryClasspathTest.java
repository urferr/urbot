package com.embabel.urbot;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Lightweight regression guard for <a href="https://github.com/embabel/urbot/issues/10">issue #10</a>: "Spring Boot startup fails when Embabel Agent {@code 0.3.5} starters are
 * mixed with {@code 0.4.0} API/RAG artifacts."
 *
 * <p>Root cause: the BYOK model factory {@code AnthropicModelFactory} (from the Anthropic autoconfigure) implemented {@code com.embabel.agent.spi.ByokFactory} — a class absent
 * from the resolved {@code embabel-agent-api} — so Spring's {@code ConfigurationClassParser} threw {@code FileNotFoundException} reading its interfaces and the application never
 * started.
 *
 * <p>This reproduces that failure at its source — resolving the factory's implemented interfaces — with no Spring context, Neo4j, web server, or API keys, so it runs in the
 * normal unit build (it is a {@code *Test}, not an excluded {@code *IT}) in milliseconds. The model starters are Maven-profile-gated
 * ({@code ANTHROPIC_API_KEY}/{@code OPENAI_API_KEY}), so a factory whose starter isn't on the classpath is skipped — issue #10 can only occur when that starter is present, which
 * is exactly when this check runs.
 */
class ByokFactoryClasspathTest {

   // BYOK model factories whose unresolvable ByokFactory interface caused issue #10. Referenced by
   // name, not import, because their starters are Maven-profile-gated and may be off the classpath.
   private static final List<String> BYOK_MODEL_FACTORIES = List.of("com.embabel.agent.config.models.anthropic.AnthropicModelFactory");

   /**
    * Loads each BYOK model factory that is on the classpath and resolves its implemented interfaces.
    * If an issue #10 version skew has regressed — the factory's {@code ByokFactory} interface missing
    * from the resolved {@code embabel-agent-api} — class linking or {@link Class#getInterfaces()}
    * throws {@link NoClassDefFoundError}/{@link TypeNotPresentException} and the test fails with a
    * pointed message. Each loaded factory is asserted to actually implement a {@code ByokFactory}.
    */
   @Test
   void byokModelFactoriesResolveTheirInterfaces() {

      int checked = 0;
      for (final String fqn : BYOK_MODEL_FACTORIES) {
         final Class<?> factory;
         try {

            // Loading the class links its `implements ByokFactory` clause; a version skew like
            // issue #10 (ByokFactory missing from the resolved api) fails to link here.
            factory = Class.forName(fqn);
         } catch (final ClassNotFoundException starterAbsent) {

            continue; // model profile inactive — its starter isn't on the classpath
         } catch (final NoClassDefFoundError skew) {

            fail("Loading " + fqn + " failed to link a referenced class — the issue #10 ByokFactory classpath skew has regressed: " + skew);
            return;
         }

         try {

            // Force resolution of the implemented interfaces too.
            final Class<?>[] interfaces = factory.getInterfaces();
            assertTrue(Arrays.stream(interfaces).anyMatch(i -> "ByokFactory".equals(i.getSimpleName())), fqn + " is expected to implement a ByokFactory");
            checked++;
         } catch (final NoClassDefFoundError | TypeNotPresentException skew) {

            fail(fqn + " could not resolve its interfaces — the issue #10 ByokFactory classpath skew has regressed: " + skew);
            return;
         }
      }

      assumeTrue(checked > 0, "No BYOK model starter on the classpath (ANTHROPIC_API_KEY profile inactive); issue #10 cannot occur without it");
   }
}
