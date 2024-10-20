package org.example.resources

import com.codahale.metrics.annotation.Timed
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.example.model.Posts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class PostCreateParams(@field:Size(min = 1, max = 256) val title: String, val content: String? = null)

data class PostUpdateParams(@field:Size(min = 1, max = 256) val title: String?, val content: String?)

@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Timed
class PostsResource() {
    @GET
    fun index(): List<Map<String, Any?>> = transaction {
        Posts.selectAll().map { row -> toMap(row) }
    }

    @GET
    @Path("/{id}")
    fun show(@PathParam("id") postId: Int): Map<String, Any?> = transaction {
        val row = Posts.selectAll().where { Posts.id eq postId }.singleOrNull()
        if (row == null) {
            throw NotFoundException()
        }
        toMap(row)
    }

    @POST
    fun create(@Valid params: PostCreateParams): Map<String, Any?> = transaction {
        val post = Posts.insert {
            it[title] = params.title
            it[content] = params.content
        }
        toMap(post.resultedValues!!.first())
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Int, @Valid params: PostUpdateParams): Int = transaction {
        val updateCount = Posts.update({ Posts.id eq id }) {
            if (params.title != null) it[title] = params.title
            it[content] = params.content
        }
        if (updateCount == 0)
            throw NotFoundException()
        updateCount
    }

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Int): Int = transaction {
        val updateCount = Posts.deleteWhere { Posts.id eq id  }
        if (updateCount == 0)
            throw NotFoundException()
        updateCount
    }

    private fun toMap(row: ResultRow): Map<String, Any?> =
        Posts.columns.associate { column -> column.name to row[column] }
}
