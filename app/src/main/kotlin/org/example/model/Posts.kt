package org.example.model;

import org.jetbrains.exposed.sql.*

object Posts : Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val title: Column<String> = varchar("title", 256)
    val content: Column<String?> = text("content").nullable()

    override val primaryKey = PrimaryKey(id)
}
