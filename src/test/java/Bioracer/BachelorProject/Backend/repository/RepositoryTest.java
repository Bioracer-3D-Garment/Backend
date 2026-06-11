package Bioracer.BachelorProject.Backend.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * Shared configuration for repository tests: an in-memory H2 database with
 * NON_KEYWORDS=USER (the "user" table name is a reserved word in H2),
 * Hibernate-generated schema, and the Postgres-specific Flyway migrations
 * disabled.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:repositorytest;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER;INIT=CREATE SCHEMA IF NOT EXISTS bioracer",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface RepositoryTest {
}
