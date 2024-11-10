#!/usr/bin/env python3

import faker
import hashlib
import uuid
import random
from faker.providers import company, date_time
import datetime
import psycopg

session = psycopg.Connection.connect("dbname=maweituo user=maweituo password=maweituo host='127.0.0.1'")

TAGS = 10
USERS = 100
ADS = 500

fake = faker.Faker()
fake.add_provider(company)
fake.add_provider(date_time)

tags = ["car", "house", "job", "clothes", "tickets", "shoes", "furniture", "website", "pet", "electronics"]

users = []
for i in range(USERS):
    id = uuid.uuid4()
    name = fake.name()
    email = fake.email()
    raw_password = id
    salt = uuid.uuid4()
#     hashed_salted_password = hashlib.sha256((str(raw_password) + str(salt)).encode("utf-8")).hexdigest()
    hashed_salted_password = "bb2a8dcbfa08b0f23f287555a324ec07148e0c59c3b4245e2f29211636b1b09e"
    salt = "ab88750c-37e6-4d92-b975-872cde5cb677"

    at = fake.past_datetime()
    session.execute("insert into users (id, name, email, password, salt, created_at, updated_at) values (%s, %s, %s, %s, %s, current_timestamp, current_timestamp)",
                    (id, name, email, hashed_salted_password, str(salt)))
    users.append(id)

ads = []
for i in range(ADS):
    author = random.choice(users)
    id = uuid.uuid4()
    title = fake.bs()
    tag_count = random.randint(1, 4)
    t = random.sample(tags, tag_count)

    session.execute("insert into advertisements (id, title, author_id, is_resolved, created_at, updated_at) values (%s, %s, %s, false, current_timestamp, current_timestamp)",
                    (id, title, author))

session.commit()
