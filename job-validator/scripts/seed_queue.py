import redis
import re

# Connect to local Redis server
r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

QUEUE_NAME = "queue:slugs:talentera"

def generate_slugs(raw_name):
    # 1. Strip Arabic and lowercase
    clean = re.sub(r'[\u0600-\u06FF]', '', raw_name.lower())
    
    # 2. Remove common corporate and legal suffixes
    clean = re.sub(r'\b(llc|ltd|co|inc|sal|wll|fzco|fz-llc|group|company|corporation|llp)\b', '', clean)
    
    # 3. Replace punctuation/symbols with spaces
    clean = re.sub(r'[^a-z0-9\s]', ' ', clean)
    
    # 4. REMOVE standalone numbers (kills "337 11 107" but keeps "b2b")
    clean = re.sub(r'\b\d+\b', '', clean)
    
    # 5. Clean up leftover spaces
    clean = ' '.join(clean.split()).strip()
    
    # Ignore anything that becomes completely empty or is suspiciously short
    if not clean or len(clean) < 3:
        return set()

    slugs = set()
    
    dashed = clean.replace(" ", "-")
    mashed = clean.replace(" ", "")
    
    # Apply DNS reality checks (must be between 3 and 63 characters)
    if 2 < len(dashed) < 63: 
        slugs.add(dashed)
    if 2 < len(mashed) < 63: 
        slugs.add(mashed)
    
    # Add the first word isolated, but only if it's a real word
    if " " in clean:
        first_word = clean.split(" ")[0]
        if len(first_word) >= 3: 
            slugs.add(first_word)

    return slugs

print("🚀 Reading raw_me_companies.txt and pushing UNIQUE slugs to Redis Set...")

total_pushed = 0
# Pointing to ../data/raw_me_companies.txt
with open("../data/raw_me_companies.txt", "r", encoding="utf-8") as f:
    for line in f:
        company = line.strip()
        if company:
            slugs = generate_slugs(company)
            for slug in slugs:
                # Use sadd to enforce atomic deduplication in Redis
                if r.sadd(QUEUE_NAME, slug):
                    total_pushed += 1

print(f"🎉 Done! Pushed {total_pushed} unique target slugs into Redis Set key: '{QUEUE_NAME}'")