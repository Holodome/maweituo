drop keyspace if exists rec;
create keyspace rec with replication = {'class':'SimpleStrategy', 'replication_factor':1};

create table rec.user_bought (
    id uuid primary key,
    ads set<uuid>
);

create table rec.user_seen (
    id uuid primary key,
    ads set<uuid>
);

create table rec.user_discussed (
    id uuid primary key,
    ads set<uuid>
);