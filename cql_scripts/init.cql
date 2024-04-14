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

create table local.advertisements (
    id uuid primary key,
    author_id uuid,
    title text,
    tags set<text>,
    images set<uuid>,
    chats set<uuid>
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
) with clustering order by (at desc);

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

create table recs.user_bought_transactional (
    id uuid primary key,
    ad uuid
);

create table recs.user_clicked_transactional (
    id uuid primary key,
    ad uuid
);

create table recs.user_discussed_transactional (
    id uuid primary key,
    ad uuid
);

create table recs.user_bought_snapshot (
    id uuid primary key,
    ads frozen<set<uuid>>
);

create table recs.user_clicked_snapshot (
    id uuid primary key,
    ads frozen<set<uuid>>
);

create table recs.user_discussed_snapshot (
    id uuid primary key,
    ads frozen<set<uuid>>
);

create table recs.user_weights (
    id uuid primary key,
    weights frozen<list<float>>
);

create table recs.tags (
    idx int primary key,
    tag text
);

create table recs.ads_snapshot (
    tag text primary key,
    ads frozen<list<uuid>>
);

create table recs.ads_tags_snapshot (
    id uuid primary key,
    tags frozen<set<text>>
);