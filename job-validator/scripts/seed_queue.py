import redis
import re

# Connect to local Redis server
r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

QUEUE_NAME = "workable:validation:queue"

def generate_slugs(raw_name):
    # Strip Arabic, lowercase, clean legal suffixes
    clean = re.sub(r'[\u0600-\u06FF]', '', raw_name.lower())
    clean = re.sub(r'\b(llc|ltd|co|inc|sal|wll|fzco|fz-llc|group|company)\b', '', clean)
    clean = re.sub(r'[^a-z0-9\s]', ' ', clean)
    clean = ' '.join(clean.split()).strip()
    
    if not clean:
        return set()

    slugs = set()
    slugs.add(clean.replace(" ", "-"))  # e.g., talabat-middle-east
    slugs.add(clean.replace(" ", ""))   # e.g., talabatmiddleeast
    
    if " " in clean:
        slugs.add(clean.split(" ")[0])  # e.g., talabat

    return slugs

print("🚀 Reading raw_me_companies.txt and pushing slugs to Redis...")

total_pushed = 0
# Pointing to ../data/raw_me_companies.txt
with open("../data/raw_me_companies.txt", "r", encoding="utf-8") as f:
    for line in f:
        company = line.strip()
        if company:
            slugs = generate_slugs(company)
            for slug in slugs:
                r.lpush(QUEUE_NAME, slug)
                total_pushed += 1

print(f"🎉 Done! Pushed {total_pushed} target slugs into Redis key: '{QUEUE_NAME}'")