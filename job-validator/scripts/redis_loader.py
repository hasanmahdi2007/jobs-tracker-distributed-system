import redis
import re

# Connect to local Redis
r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

# New queue for Workable
QUEUE_NAME = "queue:slugs:greenhouse"

def generate_slugs(raw_name):
    clean = re.sub(r'[\u0600-\u06FF]', '', raw_name.lower())
    clean = re.sub(r'\b(llc|ltd|co|inc|sal|wll|fzco|fz-llc|group|company|corporation|llp)\b', '', clean)
    clean = re.sub(r'[^a-z0-9\s]', ' ', clean)
    clean = re.sub(r'\b\d+\b', '', clean)
    clean = ' '.join(clean.split()).strip()
    
    if not clean or len(clean) < 3:
        return set()

    slugs = set()
    dashed = clean.replace(" ", "-")
    mashed = clean.replace(" ", "")
    
    if 2 < len(dashed) < 63: 
        slugs.add(dashed)
    if 2 < len(mashed) < 63: 
        slugs.add(mashed)
    
    if " " in clean:
        first_word = clean.split(" ")[0]
        if len(first_word) >= 3: 
            slugs.add(first_word)

    return slugs

print(f"🚀 Reading cleaned raw_me_companies.txt and pushing to '{QUEUE_NAME}'...")

total_pushed = 0

with open("../data/raw_me_companies.txt", "r", encoding="utf-8") as f:
    for line in f:
        company = line.strip()
        if company:
            slugs = generate_slugs(company)
            for slug in slugs:
                if r.sadd(QUEUE_NAME, slug):
                    total_pushed += 1

print(f"🎉 Done! Pushed {total_pushed} unique Workable candidate slugs into Redis key: '{QUEUE_NAME}'")