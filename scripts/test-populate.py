#!/usr/bin/env python3

from cassandra.cluster import Cluster
import faker
import hashlib
import uuid
import random
from faker.providers import company

cluster = Cluster(["127.0.0.1"], port=9042)
session = cluster.connect()

row = session.execute("SELECT release_version FROM system.local").one()
print("Connected, version:", row[0])

TAGS = 10
USERS = 100
ADS = 500

fake = faker.Faker()
fake.add_provider(company)

tags = ["car", "house", "job", "clothes", "tickets", "shoes", "furniture", "website", "pet", "electronics"]
for it in tags:
    session.execute("insert into local.tags (tag) values (%s)", (it, ))

users = []
for i in range(USERS):
    id = uuid.uuid4()
    name = fake.name()
    email = fake.email()
    raw_password = id
    salt = uuid.uuid4()
    hashed_salted_password = hashlib.sha256((str(raw_password) + str(salt)).encode("utf-8")).hexdigest()

    session.execute("insert into local.users (id, name, email, password, salt) values (%s, %s, %s, %s, %s)",
                    (id, name, email, hashed_salted_password, str(salt)))
    users.append(id)

ads = []
for i in range(ADS):
    author = random.choice(users)
    id = uuid.uuid4()
    title = fake.bs()
    tag_count = random.randint(1, 4)
    t = random.sample(tags, tag_count)

    session.execute("insert into local.advertisements (id, author_id, title, tags, resolved) values (%s, %s, %s, %s, false)",
                    (id, author, title, set(t)))
    for tag in t:
        session.execute("update local.tags set ads = ads + {%s} where tag = %s",
                        (id, tag))
    session.execute("insert into recs.user_created (id, ad) values (%s, %s)",
                    (author, id))
    ads.append(id)

