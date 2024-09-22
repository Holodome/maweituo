drop keyspace if exists local;
create keyspace local with replication = {'class':'SimpleStrategy', 'replication_factor':1};

create table local.users (
    id uuid primary key,
    email text,
    name text,
    password text,
    salt text,
);

create index on local.users(name);
create index on local.users(email);

create table local.user_ads (
    user_id uuid primary key,
    ads set<uuid>
);

create table local.advertisements (
    id uuid primary key,
    author_id uuid,
    title text,
    tags set<text>,
    images set<uuid>,
    chats set<uuid>,
    resolved boolean
);

create table local.personalized_feed (
    user_id uuid,
    idx int,
    ad_id uuid,
    primary key(user_id, idx)
) with clustering order by (idx asc);

create table local.global_feed (
    at timestamp,
    ad_id uuid,
    primary key(at)
);

create table local.tags (
    tag text primary key,
    ads set<uuid>
);

create table local.chats (
    id uuid primary key,
    ad_id uuid,
    ad_author_id uuid,
    client_id uuid
);

create index on local.chats(ad_id);
create index on local.chats(client_id);

create table local.messages (
    sender_id uuid,
    chat_id uuid,
    msg text,
    at timestamp,
    primary key (chat_id, at)
) with clustering order by (at asc);

create table local.images (
    id uuid primary key,
    ad_id uuid,
    url text,
    media_type text,
    size bigint
);

drop keyspace if exists recs;
create keyspace recs with replication = {'class':'SimpleStrategy', 'replication_factor':1};

create table recs.user_bought (
    id uuid primary key,
    ad uuid
);

create table recs.user_created (
    id uuid primary key,
    ad uuid
);

create table recs.user_discussed (
    id uuid primary key,
    ad uuid
);

create role if not exists core_role with password = 'password' and login = true;
grant select on keyspace local to core_role;
grant modify on keyspace local to core_role;

create role if not exists recs_role with password = 'password' and login = true;
grant select on keyspace recs to recs_role;
grant modify on keyspace recs to recs_role;
grant select on keyspace local to recs_role;

create role if not exists administrator with superuser = true and password = 'password' and login = true;

create user if not exists core_user with password 'password';
grant core_role to core_user;

create user if not exists recs_user with password 'password';
grant recs_role to recs_user;

create user if not exists admin with password 'password';
grant administrator to admin;