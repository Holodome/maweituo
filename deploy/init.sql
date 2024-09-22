create table if not exists users (
  id uuid primary key,
  email text unique not null,
  name text unique not null,
  password text not null,
  salt text not null
);

create table if not exists advertisements (
  id uuid primary key,
  title text not null,
  author_id uuid references users(id) on delete restrict
);

create table if not exists personalized_feed (
  user_id uuid not null,
  idx int not null,
  ad_id uuid references advertisements(id),
  primary key(user_id, idx)
);

create table if not exists global_feed (
  at timestamp primary key,
  ad_id uuid references advertisements(id)
);

create table if not exists tags (
  tag_id uuid primary key,
  name text not null
);

create table if not exists tag_ads (
  tag_id uuid references tags(id), 
  ad_id uuid references advertisements(id)
  primary key(tag_id, ad_id)
);

create table if not exists chats (
  id uuid primary key,
  ad_id uuid references advertisements(id),
  ad_author_id references users(id),
  client_id uuid references users(id)
);

create index if not exists chat_ads on chats (ad_id);
create index if not exists chat_clients on chats (client_id);

create table if not exists messages (
  sender_id uuid references users(id),
  chat_id uuid references chats(id),
  msg text not null, 
  at timestamp not null,
);

create table if not exists images (
  id uuid primary key,
  ad_id uuid references advertisements(id),
  url text,
  media_type text,
  size bigint
);


