drop database if exists maweituo;
create database maweituo;

use maweituo;

create table user_weights (
    id UUID,
    weights Array(Float32)
)
engine = ReplacingMergeTree
order by id;

create table user_bought (
    id UUID,
    ads Array(UUID)
)
engine = ReplacingMergeTree
order by id;

create table user_created (
    id UUID,
    ads Array(UUID)
)
engine = ReplacingMergeTree
order by id;

create table user_discussed (
    id UUID,
    ads Array(UUID)
)
engine = ReplacingMergeTree
order by id;

create table tag_ads (
    tag text,
    ads Array(UUID)
)
engine = ReplacingMergeTree
order by tag;

create table ad_tags (
    id UUID,
    tags Array(text)
)
engine = ReplacingMergeTree
order by id;
