package org.example

import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.configuration.ResourceConfigurationSourceProvider
import io.dropwizard.core.Application
import io.dropwizard.core.setup.Bootstrap
import io.dropwizard.core.setup.Environment
import io.dropwizard.jersey.validation.ConstraintMessage
import io.dropwizard.jersey.validation.JerseyViolationException
import io.dropwizard.util.Resources
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ConstraintViolation
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import org.example.model.Posts
import org.example.resources.PostsResource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

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

class ServeIndexServlet: HttpServlet() {
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

        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver", user = "root", password = "")

        transaction {
            SchemaUtils.create(Posts)
            Posts.insert {
                it[title] = "Introduction to kotlin"
                it[content] = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum"
            }
        }
    }

    override fun run(configuration: BlogConfiguration, environment: Environment) {
        environment.servlets().addServlet("spa", ServeIndexServlet()).addMapping("/*")
        environment.jersey().register(PostsResource())
        environment.jersey().register(CustomJerseyViolationExceptionMapper())
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val defaults = arrayOf("server", "config.yml")

            BlogApplication().run(*if (args.isEmpty()) defaults else args)
        }
    }

}
