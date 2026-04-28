FROM postgres:16-alpine

COPY src/main/resources/sql/init_scheme.sql /docker-entrypoint-initdb.d/01-init_schema.sql
COPY src/main/resources/sql/init_data.sql /docker-entrypoint-initdb.d/02-init_data.sql
