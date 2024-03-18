create keyspace local with replication = {'class':'SimpleStrategy', 'replication_factor':1};

create table local.users (
    id uuid primary key,
    email text,
    name text,
    password text,
    salt text,
    created_at timestamp,
    updated_at text
);