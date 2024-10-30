package org.example

import com.fasterxml.jackson.databind.JsonNode
import io.dropwizard.testing.junit5.DropwizardAppExtension
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.GenericType
import jakarta.ws.rs.core.Response
import org.example.model.Post
import org.example.model.PostCreateParams
import org.example.model.PostsDAO
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(DropwizardExtensionsSupport::class)
class PostsResourceAcceptanceTest {
    var application = DropwizardAppExtension(
        BlogApplication::class.java,
        "config/test.yml"
    )

    inline fun <reified T> Response.readEntity(): T =
        readEntity(object : GenericType<T>() {})

    fun apiClient() = application.client().target(
        String.format("http://localhost:%d/api", application.localPort)
    )

    @BeforeEach
    fun beforeEach(): Unit {
        PostsDAO.deleteAll()
    }

    @Test
    fun indexWithNoPosts() {
        val response = apiClient().path("/posts")
            .request()
            .get()
            .readEntity<List<Post>>()

        assert(response.isEmpty())
    }

    @Test
    fun indexWithPosts() {
        PostsDAO.create(PostCreateParams(title = "hello", content = "world"))

        val response = apiClient().path("/posts")
            .request()
            .get()
            .readEntity<List<Post>>()

        assert(response.isNotEmpty())
        assertEquals("hello", response.first().title)
    }

    @Test
    fun showNotFound() {
        val response = apiClient().path("/posts/1")
            .request()
            .get()

        assertEquals(404, response.status)
    }

    @Test
    fun showPost() {
        val postId = PostsDAO.create(PostCreateParams(title = "hello", content = "world")).id

        val response = apiClient().path(String.format("/posts/%d", postId))
            .request()
            .get()
            .readEntity<Post>()

        assertEquals("hello", response.title)
    }

    @Test
    fun createPost() {
        val response = apiClient().path("/posts")
            .request()
            .post(Entity.json(PostCreateParams(title = "hello", content = "bla")))
            .readEntity<Post>()

        assertEquals("hello", response.title)
    }

    @Test
    fun createPostInvalidParams() {
        val response = apiClient().path("/posts")
            .request()
            .post(Entity.json(PostCreateParams(title = "", content = "bla")))
            .readEntity<JsonNode>()

        assertEquals("Validation error", response.get("message").asText())
        assertEquals("title", response.get("errors").get(0).get("field").asText())
        assertEquals("size must be between 1 and 256", response.get("errors").get(0).get("message").asText())
    }
}
