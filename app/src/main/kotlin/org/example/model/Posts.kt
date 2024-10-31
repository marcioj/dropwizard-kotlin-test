package org.example.model;

import jakarta.validation.constraints.Size
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object Posts : Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val title: Column<String> = varchar("title", 256)
    val content: Column<String> = text("content")

    override val primaryKey = PrimaryKey(id)
}

data class Post(var id: Int, var title: String, var content: String)

data class PostCreateParams(@field:Size(min = 1, max = 256) val title: String, @field:Size(min = 1) val content: String)

data class PostUpdateParams(@field:Size(min = 1, max = 256) val title: String?, @field:Size(min = 1) val content: String?)

object PostsDAO {
    fun list() = transaction {
        Posts.selectAll().map(::toModel)
    }

    fun findOrNull(postId: Int) = transaction {
        Posts.selectAll().where { Posts.id eq postId }.singleOrNull()?.let { toModel(it) }
    }

    fun create(params: PostCreateParams) = transaction {
        val post = Posts.insert {
            it[title] = params.title
            it[content] = params.content
        }
        toModel(post.resultedValues!!.first())
    }

    fun update(id: Int, params: PostUpdateParams) = transaction {
        val updateCount = Posts.update({ Posts.id eq id }) {
            if (params.title != null) it[title] = params.title
            if (params.content != null) it[content] = params.content
        }
        updateCount > 0
    }

    fun delete(id: Int) = transaction {
        Posts.deleteWhere { Posts.id eq id } > 0
    }

    fun deleteAll() = transaction {
        Posts.deleteAll()
    }

    fun count(): Long = transaction {
        Posts.select(Posts.id.count()).first()[Posts.id.count()]
    }

    private fun toModel(row: ResultRow) =
        Post(id = row[Posts.id], title = row[Posts.title], content = row[Posts.content])
}
