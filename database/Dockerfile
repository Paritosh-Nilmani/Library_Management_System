FROM mysql:8.4

# The official MySQL image runs this on first initialization of /var/lib/mysql.
COPY database/schema.sql /docker-entrypoint-initdb.d/001-library-schema.sql