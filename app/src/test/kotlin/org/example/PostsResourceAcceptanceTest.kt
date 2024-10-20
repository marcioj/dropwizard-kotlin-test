package org.example

import com.fasterxml.jackson.databind.JsonNode
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit5.DropwizardAppExtension
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.GenericType
import org.example.model.Posts
import org.example.resources.PostCreateParams
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals


@ExtendWith(DropwizardExtensionsSupport::class)
class PostsResourceAcceptanceTest {
    var application = DropwizardAppExtension(
        BlogApplication::class.java,
        "config.yml"
    )

    fun apiClient() = application.client().target(
        String.format("http://localhost:%d/api", application.localPort))

    @BeforeEach
    fun beforeEach(): Unit {
        transaction {
            Posts.deleteAll()
        }
    }

    @Test fun indexWithNoPosts() {
        val response = apiClient().path("/posts")
            .request()
            .get()
            .readEntity(object: GenericType<List<Map<String, Any?>>>() {})

        assert(response.isEmpty())
    }

    @Test fun indexWithPosts() {
        transaction {
            Posts.insert { it[Posts.title] ="hello" }
        }

        val response = apiClient().path("/posts")
            .request()
            .get()
            .readEntity(object: GenericType<List<Map<String, Any?>>>() {})

        assert(response.isNotEmpty())
        assertEquals("hello", response.first()["title"])
    }

    @Test fun showNotFound() {
        val response = apiClient().path("/posts/1")
            .request()
            .get()

        assertEquals(404, response.status)
    }

    @Test fun showPost() {
        val postId = transaction {
            Posts.insert { it[Posts.title] ="hello" }.resultedValues!!.first()[Posts.id]
        }

        val response = apiClient().path(String.format("/posts/%d", postId))
            .request()
            .get()
            .readEntity(object: GenericType<Map<String, Any?>>() {})

        assertEquals("hello", response["title"])
    }

    @Test fun createPost() {
        val response = apiClient().path("/posts")
            .request()
            .post(Entity.json(PostCreateParams(title = "hello")))
            .readEntity(object: GenericType<Map<String, Any?>>() {})

        assertEquals("hello", response["title"])
    }

    @Test fun createPostInvalidParams() {
        val response = apiClient().path("/posts")
            .request()
            .post(Entity.json(PostCreateParams(title = "")))
            .readEntity(JsonNode::class.java)

        assertEquals("Validation error", response.get("message").asText())
        assertEquals("title", response.get("errors").get(0).get("field").asText())
        assertEquals("size must be between 1 and 256", response.get("errors").get(0).get("message").asText())
    }
}
