create keyspace local with replication = {'class':'SimpleStrategy', 'replication_factor':1};

create table local.users (
    id uuid primary key,
    email text,
    name text,
    password text,
    salt text,
    created_at timestamp,
    updated_at text,
    ads list<uuid>
);

create table local.advertisements (
    id uuid primary key,
    author_id uuid,
    title text,
    tags set<text>,
    images set<uuid>,
    chats set<uuid>
);

create table local.chats (
    id uuid primary key,
    ad_id uuid,
    ad_author_id uuid,
    client_id uuid
);

create table local.messages (
    sender_id uuid,
    chat_id uuid,
    msg text,
    at timestamp
    primary key ((chat_id, at))
) with clustering order by (at asc);

create table local.images (
    id uuid primary key,
    ad_id uuid,
    url text
);