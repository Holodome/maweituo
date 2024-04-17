create table user_weights (
    id uuid,
    weights Array(Float32)
)
engine = ReplacingMergeTree
order by id;

create table user_bought (
    id uuid,
    ads Array(uuid)
)
engine = ReplacingMergeTree
order by id;

create table user_created (
    id uuid,
    ads Array(uuid)
)
engine = ReplacingMergeTree
order by id;

create table user_discussed (
    id uuid,
    ads Array(uuid)
)
engine = ReplacingMergeTree
order by id;

create table tag_ads (
    tag text,
    ads Array(uuid)
)
engine = ReplacingMergeTree
order by tag;

create table ad_tags (
    id uuid,
    tags Array(text)
)
engine = ReplacingMergeTree
order by tag;
