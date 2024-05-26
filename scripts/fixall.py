#!/usr/bin/env python3

from cassandra.cluster import Cluster
import faker
import hashlib
import uuid
import random
from faker.providers import company, date_time
import datetime

cluster = Cluster(["127.0.0.1"], port=9042)
session = cluster.connect()

row = session.execute("SELECT release_version FROM system.local").one()
print("Connected, version:", row[0])

ads = session.execute("SELECT id, ad_id FROM local.chats")
for chatId, ad_id in ads:
  session.execute("update local.advertisements set chats = chats + { %s } where id = %s", (chatId, ad_id))

# ads = session.execute("SELECT id, author_id FROM local.advertisements")
# for adId, authorId in ads:
#   session.execute("update local.user_ads set ads = ads + { %s } where user_id = %s", (adId, authorId))