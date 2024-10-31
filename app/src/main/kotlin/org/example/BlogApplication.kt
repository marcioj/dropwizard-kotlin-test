package org.example

import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.dropwizard.configuration.ResourceConfigurationSourceProvider
import io.dropwizard.core.Application
import io.dropwizard.core.setup.Bootstrap
import io.dropwizard.core.setup.Environment
import io.dropwizard.jersey.validation.ConstraintMessage
import io.dropwizard.jersey.validation.JerseyViolationException
import io.dropwizard.util.Resources
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ConstraintViolation
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.example.resources.PostsResource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import java.util.*


class CustomJerseyViolationExceptionMapper : ExceptionMapper<JerseyViolationException> {
    override fun toResponse(exception: JerseyViolationException): Response {
        val violations: Set<ConstraintViolation<*>> = exception.constraintViolations
        val invocable = exception.invocable
        val status = ConstraintMessage.determineStatus(violations, invocable)

        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(object {
                val code = status
                val message = "Validation error"
                val errors = violations.map {
                    object {
                        val field = ConstraintMessage.isRequestEntity(it, invocable).get()
                        val message = it.message
                    }
                }
            })
            .build()
    }
}

class ServeIndexServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val resource = Resources.getResource("assets/index.html")
        resp.outputStream.write(resource.readBytes())
    }
}

class BlogApplication : Application<BlogConfiguration>() {
    override fun getName(): String {
        return "blog"
    }

    override fun initialize(bootstrap: Bootstrap<BlogConfiguration>) {
        bootstrap.configurationSourceProvider = ResourceConfigurationSourceProvider()
        bootstrap.objectMapper.registerModule(kotlinModule())
    }

    override fun run(configuration: BlogConfiguration, environment: Environment) {
        setupCORS(environment)
//        environment.servlets().addServlet("spa", ServeIndexServlet()).addMapping("/*")
        environment.jersey().register(PostsResource())
        environment.jersey().register(CustomJerseyViolationExceptionMapper())
        setupDB(configuration, environment)
    }

    private fun setupDB(configuration: BlogConfiguration, environment: Environment) {
        val dataSource = configuration.database.build(environment.metrics(), "database")
        Database.connect(dataSource)
        environment.lifecycle().manage(dataSource)

        val flyway = Flyway.configure().dataSource(dataSource).load()
        flyway.migrate()
    }

    private fun setupCORS(environment: Environment) {
        // Enable CORS headers
        val cors = environment.servlets().addFilter("CORS", CrossOriginFilter::class.java)

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*")
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin")
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD")

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")

        // DO NOT pass a preflight request to down-stream auth filters
        // unauthenticated preflight requests should be permitted by spec
        cors.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val defaults = arrayOf("server", "config/development.yml")

            BlogApplication().run(*if (args.isEmpty()) defaults else args)
        }
    }
}
