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
import org.example.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Timed
class PostsResource() {
    @GET
    fun index() = PostsDAO.list()

    @GET
    @Path("/{id}")
    fun show(@PathParam("id") postId: Int): Post =
        PostsDAO.findOrNull(postId) ?: throw NotFoundException()

    @POST
    fun create(@Valid params: PostCreateParams): Post = PostsDAO.create(params)

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Int, @Valid params: PostUpdateParams): Post {
        if (!PostsDAO.update(id, params))
            throw NotFoundException()
        return PostsDAO.findOrNull(id)!!
    }

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Int): Post {
        val post = PostsDAO.findOrNull(id) ?: throw NotFoundException()
        PostsDAO.delete(id)
        return post
    }

    private fun toModel(row: ResultRow) =
        Post(id = row[Posts.id], title = row[Posts.title], content = row[Posts.content])
}
