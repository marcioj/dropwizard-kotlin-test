package org.example;

import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory

class BlogConfiguration : Configuration() {
    var database = DataSourceFactory()
}
